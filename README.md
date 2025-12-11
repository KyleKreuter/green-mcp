# Green MCP Server

A Model Context Protocol (MCP) server for semantic search over political resolutions (Beschlüsse) from Grüne Hamburg. Built with Spring Boot and pgvector for vector similarity search.

## Features

- **Semantic Search**: Find relevant resolutions using natural language queries
- **Document-specific Search**: Search within a specific resolution document
- **Document Listing**: List all available resolution documents
- **Vector Similarity**: Uses pgvector with cosine similarity for accurate results

## Prerequisites

- Docker and Docker Compose
- Mistral AI API key (for query embeddings)
- Node.js (for Claude Desktop integration via mcp-remote)

## Quick Start

### 1. Clone and Configure

```bash
git clone https://github.com/KyleKreuter/green-mcp.git
cd green-mcp
```

Create a `.env` file with your Mistral AI API key:

```bash
nano .env
# Edit .env and add your MISTRAL_API_KEY
```

### 2. Add Embedding Data

The server requires pre-computed embeddings to function. Place these files in `src/main/resources/data/`:

- `embeddings.csv` - Contains document chunks with embeddings
- `metadata.csv` - Contains metadata for each chunk

**CSV Format for `embeddings.csv`:**
```csv
"id","pdf_url","chunk_index","content","embedding"
"uuid","https://...","0","Text content here","[0.1, 0.2, ...]"
```

**CSV Format for `metadata.csv`:**
```csv
"id","filename","title","topic","sequence_number","word_count","created_at"
"uuid","document.pdf","Title","Topic","0","150","2024-01-01T00:00:00"
```

> **Note**: Embeddings must be 1024-dimensional vectors (Mistral AI embedding format).
>
> To obtain the Grüne Hamburg embeddings dataset, please contact me.
>
> To create your own embeddings, use the Mistral AI Embeddings API.

### 3. Start the Server

```bash
docker compose up -d --build
```

The MCP server will be available at `http://localhost:2228/sse`

### 4. Connect to Claude Desktop

Add to your Claude Desktop configuration (`~/Library/Application Support/Claude/claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "green-mcp": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "http://localhost:2228/sse"]
    }
  }
}
```

Restart Claude Desktop to load the MCP server.

## Available Tools

### `beschluesseSuchen`
Search across all resolutions using semantic similarity.

**Parameters:**
- `query` (string): Natural language search query
- `limit` (integer, optional): Number of results (1-20, default: 5)

**Example:** "Find resolutions about climate protection"

### `inBeschlussSuchen`
Search within a specific resolution document.

**Parameters:**
- `beschlussName` (string): Filename or part of filename
- `query` (string): Natural language search query
- `limit` (integer, optional): Number of results (1-20, default: 5)

**Example:** Search for "renewable energy" in "Wahlprogramm-2024.pdf"

### `beschluesseListen`
List all available resolution documents.

**Parameters:** None

**Returns:** List of all document filenames

## Architecture

```
┌─────────────────┐         ┌──────────────────┐         ┌─────────────┐
│  Claude Desktop │◄──SSE──►│  Spring Boot     │◄───────►│  PostgreSQL │
│  (MCP Client)   │         │  MCP Server      │         │  + pgvector │
└─────────────────┘         └──────────────────┘         └─────────────┘
                                    │
                                    ▼
                            ┌──────────────────┐
                            │   Mistral AI     │
                            │   (Embeddings)   │
                            └──────────────────┘
```

**Data Flow:**
1. User query → Mistral AI → Query embedding (1024 dim)
2. Query embedding → pgvector cosine similarity search
3. Top-K results → MCP response to Claude

## Development

### Local Development

```bash
# Start only PostgreSQL
docker compose up -d postgres

# Run Spring Boot locally
MISTRAL_API_KEY=your-key ./mvnw spring-boot:run
```

### Running Tests

```bash
./mvnw test
```

### Building

```bash
./mvnw package -DskipTests
```

## Configuration

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `MISTRAL_API_KEY` | Mistral AI API key for embeddings | Yes |
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | No (default in Docker) |
| `SPRING_DATASOURCE_USERNAME` | Database username | No (default: greenmcp) |
| `SPRING_DATASOURCE_PASSWORD` | Database password | No (default: greenmcp) |

### Application Properties

Key settings in `application.properties`:

```properties
server.port=2228
spring.ai.mcp.server.sse-message-endpoint=/mcp/message
spring.jpa.hibernate.ddl-auto=update
```

## Creating Your Own Embeddings

To use this MCP server with your own documents:

1. **Extract text** from your PDF documents
2. **Chunk the text** into meaningful segments (recommended: 200-500 words per chunk)
3. **Generate embeddings** using Mistral AI's embedding API:

```python
from mistralai.client import MistralClient

client = MistralClient(api_key="your-key")
response = client.embeddings(
    model="mistral-embed",
    input=["Your text chunk here"]
)
embedding = response.data[0].embedding  # 1024-dimensional vector
```

4. **Create CSV files** in the required format (see above)
5. **Place files** in `src/main/resources/data/`

## License

MIT

## Contact

For questions about the Grüne Hamburg embeddings dataset or this project, please open an issue.
