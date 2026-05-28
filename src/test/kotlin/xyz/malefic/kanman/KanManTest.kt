package xyz.malefic.kanman

import io.kotest.matchers.shouldBe
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.kotest.shouldHaveStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class KanManTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var storage: EntryStorage
    private lateinit var testApp: HttpHandler

    @BeforeEach
    fun setup() {
        storage = EntryStorage(tempDir.toString())
        testApp = createApp(storage, apiKey = null)
    }

    @Test
    fun `Ping test`() {
        val response = testApp(Request(GET, "/api/ping"))
        response shouldHaveStatus OK
        response.bodyString() shouldBe "pong"
    }

    @Test
    fun `Health check test`() {
        val response = testApp(Request(GET, "/api/health"))
        response shouldHaveStatus OK
        response.bodyString() shouldBe "healthy"
    }

    @Test
    fun `Post and get entry by id`() {
        val newEntry = Entry(author = "Author", text = "This is a new entry")
        val postRequest = Request(POST, "/api/entries").with(entryLens of newEntry)
        val postResponse = testApp(postRequest)

        postResponse shouldHaveStatus CREATED
        val responseEntry = entryLens(postResponse)
        responseEntry.id shouldBe 1L
        responseEntry.author shouldBe newEntry.author
        responseEntry.text shouldBe newEntry.text

        val getResponse = testApp(Request(GET, "/api/entries/${responseEntry.id}"))

        getResponse shouldHaveStatus OK
        entryLens(getResponse) shouldBe responseEntry
    }

    @Test
    fun `Post entry accepts null id and assigns server id`() {
        val postRequest =
            Request(POST, "/api/entries")
                .body("""{"id":null,"author":"Author","text":"Entry with null id"}""")
        val postResponse = testApp(postRequest)

        postResponse shouldHaveStatus CREATED
        val responseEntry = entryLens(postResponse)
        responseEntry.id shouldBe 1L
        responseEntry.author shouldBe "Author"
        responseEntry.text shouldBe "Entry with null id"
    }

    @Test
    fun `Delete entry removes it from storage and returns no content`() {
        val newEntry = Entry(author = "Author", text = "To be deleted")
        val savedEntry = entryLens(testApp(Request(POST, "/api/entries").with(entryLens of newEntry)))

        val deleteResponse = testApp(Request(DELETE, "/api/entries/${savedEntry.id}"))

        deleteResponse shouldHaveStatus NO_CONTENT
        deleteResponse.bodyString() shouldBe ""
        storage.loadHistory().isEmpty() shouldBe true

        val getResponse = testApp(Request(GET, "/api/entries/${savedEntry.id}"))
        getResponse shouldHaveStatus NOT_FOUND
        getResponse.bodyString() shouldBe "No entry found with id ${savedEntry.id}"
    }

    @Test
    fun `Delete entry with invalid id returns bad request`() {
        val deleteResponse = testApp(Request(DELETE, "/api/entries/does-not-exist"))

        deleteResponse shouldHaveStatus BAD_REQUEST
        deleteResponse.bodyString() shouldBe "Invalid ID format, expected a number"
    }

    @Test
    fun `Delete entry for unknown id returns not found`() {
        val deleteResponse = testApp(Request(DELETE, "/api/entries/9999"))

        deleteResponse shouldHaveStatus NOT_FOUND
        deleteResponse.bodyString() shouldBe "No entry found with id 9999"
    }

    @Test
    fun `Put entry updates existing entry`() {
        val savedEntry =
            entryLens(
                testApp(
                    Request(POST, "/api/entries")
                        .with(entryLens of Entry(author = "Original", text = "Original text")),
                ),
            )

        val updated = Entry(author = "Updated", text = "Updated text", date = savedEntry.date)
        val putResponse = testApp(Request(PUT, "/api/entries/${savedEntry.id}").with(entryLens of updated))

        putResponse shouldHaveStatus OK
        val updatedEntry = entryLens(putResponse)
        updatedEntry.id shouldBe savedEntry.id
        updatedEntry.author shouldBe "Updated"
        updatedEntry.text shouldBe "Updated text"
    }

    @Test
    fun `Entry persists after server restart`() {
        val newEntry = Entry(author = "Persistence Author", text = "This entry should persist")
        val postRequest = Request(POST, "/api/entries").with(entryLens of newEntry)
        val postResponse = testApp(postRequest)
        val savedEntry = entryLens(postResponse)
        savedEntry.id shouldBe 1L

        val newStorage = EntryStorage(tempDir.toString())
        val newApp = createApp(newStorage, apiKey = null)

        val getResponse = newApp(Request(GET, "/api/entries/${savedEntry.id}"))

        getResponse shouldHaveStatus OK
        entryLens(getResponse) shouldBe savedEntry
    }

    @Test
    fun `Deleted entry does not reappear after restart`() {
        val newEntry = Entry(author = "Author", text = "Persisted then deleted")
        val savedEntry = entryLens(testApp(Request(POST, "/api/entries").with(entryLens of newEntry)))

        val deleteResponse = testApp(Request(DELETE, "/api/entries/${savedEntry.id}"))
        deleteResponse shouldHaveStatus NO_CONTENT

        val newStorage = EntryStorage(tempDir.toString())
        val newApp = createApp(newStorage, apiKey = null)

        newStorage.loadHistory().isEmpty() shouldBe true

        val getResponse = newApp(Request(GET, "/api/entries/${savedEntry.id}"))
        getResponse shouldHaveStatus NOT_FOUND
        getResponse.bodyString() shouldBe "No entry found with id ${savedEntry.id}"
    }

    @Test
    fun `Get entries returns empty list when no entries exist`() {
        val getResponse = testApp(Request(GET, "/api/entries"))

        getResponse shouldHaveStatus OK
        val entries = entryListLens(getResponse)
        entries.isEmpty() shouldBe true
    }

    @Test
    fun `Entry history tracks all saved entries`() {
        val entry1 = Entry(author = "Author 1", text = "First entry")
        val entry2 = Entry(author = "Author 2", text = "Second entry")

        val savedEntry1 = entryLens(testApp(Request(POST, "/api/entries").with(entryLens of entry1)))
        val savedEntry2 = entryLens(testApp(Request(POST, "/api/entries").with(entryLens of entry2)))
        savedEntry1.id shouldBe 1L
        savedEntry2.id shouldBe 2L

        val historyResponse = testApp(Request(GET, "/api/entries"))
        historyResponse shouldHaveStatus OK

        val history = entryListLens(historyResponse)
        history.size shouldBe 2
        history[0].id shouldBe savedEntry1.id
        history[0].author shouldBe "Author 1"
        history[0].text shouldBe "First entry"
        history[1].id shouldBe savedEntry2.id
        history[1].author shouldBe "Author 2"
        history[1].text shouldBe "Second entry"
    }

    @Test
    fun `Post entry with songQuery searches and includes found song`() {
        val entryWithQuery = Entry(author = "Author", text = "Entry with song", songQuery = "test song")
        val postRequest = Request(POST, "/api/entries").with(entryLens of entryWithQuery)
        val postResponse = testApp(postRequest)

        postResponse shouldHaveStatus CREATED
        val responseEntry = entryLens(postResponse)

        responseEntry.songQuery shouldBe null
        responseEntry.author shouldBe "Author"
        responseEntry.text shouldBe "Entry with song"
    }

    @Test
    fun `Post entry without songQuery works normally`() {
        val entry = Entry(author = "Author", text = "Normal entry without query")
        val postRequest = Request(POST, "/api/entries").with(entryLens of entry)
        val postResponse = testApp(postRequest)

        postResponse shouldHaveStatus CREATED
        val responseEntry = entryLens(postResponse)
        responseEntry.author shouldBe "Author"
        responseEntry.text shouldBe "Normal entry without query"
        responseEntry.song shouldBe null
        responseEntry.songQuery shouldBe null
    }

    @Test
    fun `Entry with null songQuery persists correctly`() {
        val entry = Entry(author = "Author", text = "Entry with explicit null songQuery", songQuery = null)
        val postRequest = Request(POST, "/api/entries").with(entryLens of entry)
        val postResponse = testApp(postRequest)
        val savedEntry = entryLens(postResponse)

        val getResponse = testApp(Request(GET, "/api/entries/${savedEntry.id}"))
        getResponse shouldHaveStatus OK
        val retrievedEntry = entryLens(getResponse)

        retrievedEntry.author shouldBe "Author"
        retrievedEntry.text shouldBe "Entry with explicit null songQuery"
        retrievedEntry.songQuery shouldBe null
    }
}
