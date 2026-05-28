# DailyMalefic

A lightweight REST API service for managing daily journal entries with history tracking, music integration, and API key authentication.

## Features

- **Entry Management**: Store and retrieve daily journal entries via REST API
- **Music Integration**: Automatically search and associate YouTube Music songs with entries
- **Entry History**: Track all historical entries with dates
- **API Key Authentication**: Secure write endpoints (`POST`, `PUT`, `DELETE`) with API key authentication
- **Persistence**: File-based storage with automatic initialization

## Docker

Use the included `docker-compose.yml`:

```bash
export API_KEY=your-secret-key   # optional
docker compose up --build
```

Or pull and run the `maleficmarauder/dailymalefic` image directly from Docker Hub:

```bash
docker pull maleficmarauder/dailymalefic
docker run -p 6320:6320 -e API_KEY=your-secret-key -v entry_data:/data maleficmarauder/dailymalefic
```

These both start the app on port `6320`, mount `entry_data` to `/data`, and pass `API_KEY` through if you set it.

## Endpoints

All endpoints are prefixed with `/api`.

### GET /api/ping
**Purpose:** Quick health check.

**Response:**

- `200 OK`: Service is reachable. Body is `pong`.

### GET /api/auth/validate
**Purpose:** Check if API key authentication is working/correct.

**Response:**

- `200 OK`: API key is valid, or no `API_KEY` is configured on the server.
- `401 Unauthorized`: Provided `X-API-Key` is invalid.

### GET /api/health
**Purpose:** Health check for Docker.

**Response:**

- `200 OK`: Service is healthy. Body is `healthy`.

### GET /api/entries
**Purpose:** List entries.

**Request:** Optional query params:

- `date=YYYY-MM-DD` to load entries for a specific date
- `author=<author-name>` to load entries by author
- `latest=true` to load entries only from the most recent date
- `latest=false` to explicitly load full history

**Response:**

- `200 OK`: Returns an array of entries. With no query parameters, returns full history. Empty result is `[]`.
- `400 Bad Request`: `date` is not `YYYY-MM-DD`, or `latest` is not `true`/`false`.

**Examples:**

No query parameters (`GET /api/entries`):

```json
[
  {
    "id": "1",
    "author": "Author Name",
    "text": "Entry text",
    "date": "2026-05-11",
    "song": {
      "id": "song-id",
      "name": "Song Name",
      "artists": [
        {
          "id": "artist-id",
          "name": "Artist Name"
        }
      ]
    }
  }
]
```

`?date=YYYY-MM-DD`:

```json
[
  {
    "id": "1",
    "author": "Author Name",
    "text": "Entry text",
    "date": "2026-05-11",
    "song": null
  }
]
```

### GET /api/entries/{id}
**Purpose:** Get a single entry by ID.

**Response:**

- `200 OK`: Returns the requested entry object.
- `400 Bad Request`: `id` is not numeric.
- `404 Not Found`: No entry exists with that ID.

**Example:**

```json
{
  "id": "1",
  "author": "Author Name",
  "text": "Entry text",
  "date": "2026-05-11",
  "song": {
    "id": "song-id",
    "name": "Song Name",
    "artists": [
      {
        "id": "artist-id",
        "name": "Artist Name"
      }
    ]
  }
}
```

### POST /api/entries
**Purpose:** Create a new entry.

**Authentication:** Send `X-API-Key` when `API_KEY` is set.

**Request:**

- `Content-Type: application/json`
- Optional `X-API-Key: your-api-key`
- JSON body with `author`, `text`, `date`, optional `songQuery`

**Request example: new entry**
```json
{
  "author": "New Author",
  "text": "Entry text",
  "date": "2026-05-11",
  "songQuery": "song name or artist (optional - will auto-search YouTube Music)"
}
```

**Response:**

- `201 Created`: Entry created successfully. Returns the saved entry and `Location: /api/entries/{id}`.
- `400 Bad Request`: Payload is invalid, or `id` was provided in create request.
- `401 Unauthorized`: Missing/invalid `X-API-Key` while `API_KEY` is configured.

**Response example:**

```json
{
  "id": "1",
  "author": "New Author",
  "text": "Entry text",
  "date": "2026-05-11",
  "song": {
    "id": "song-id",
    "name": "Song Name",
    "artists": [
      {
        "id": "artist-id",
        "name": "Artist Name"
      }
    ]
  }
}
```

### PUT /api/entries/{id}
**Purpose:** Update an existing entry by ID.

**Authentication:** Send `X-API-Key` when `API_KEY` is set.

**Request:**

- `Content-Type: application/json`
- Optional `X-API-Key: your-api-key`
- JSON body containing updated `author`, `text`, `date`, and optional `songQuery`

**Request example:**
```json
{
  "author": "Updated Author",
  "text": "Updated entry text",
  "date": "2026-05-11",
  "songQuery": "different song (optional)"
}
```

**Response:**

- `200 OK`: Entry updated successfully and returned in response body.
- `400 Bad Request`: Invalid payload, invalid ID format, or mismatched body/path IDs.
- `401 Unauthorized`: Missing/invalid `X-API-Key` while `API_KEY` is configured.
- `404 Not Found`: Target entry does not exist.

### DELETE /api/entries/{id}
**Purpose:** Delete an entry by ID.

**Authentication:** Send `X-API-Key` when `API_KEY` is set.

**Response:**

- `204 No Content`: Entry deleted successfully.
- `400 Bad Request`: `id` is invalid.
- `401 Unauthorized`: Missing/invalid `X-API-Key` while `API_KEY` is configured.
- `404 Not Found`: No matching entry exists.

## Configuration

### Environment Variables

- `API_KEY`: Optional API key for write operations (`POST`, `PUT`, `DELETE`). If set, requests must include `X-API-Key`. Otherwise, write endpoints are open without authentication.
- `JAVA_OPTS`: Java runtime options (default: `-Xmx512m`)

## Significant Libraries

- **HTTP Framework**: [http4k](https://www.http4k.org/)
- **Music API**: [syk-sh's ytm-kt](https://gitlab.com/syk.sh/ytm-kt)
