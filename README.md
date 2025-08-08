# Advertisement Blocker

A simple, configurable DNS proxy server written in Java that blocks known ad-serving domains. This tool intercepts DNS requests and filters out domains from a blocklist, preventing clients from resolving and connecting to ad networks and trackers.

## ✨ Features

* ✅ Intercepts and handles DNS requests (must be set as the upstream resolver)
* 🚫 Blocks known ad and tracking domains
* 📝 Supports custom blocklists
* 💾 Lightweight, few external dependencies
* 🔧 Configurable via properties file or CLI arguments
* 📄 Logs DNS queries and blocked domains

## 📦 How It Works

1. The proxy listens for DNS requests from clients.
2. For each incoming request:

   * If the domain is on the blocklist, it responds with a `BLOCKED` status code.
   * Otherwise, it forwards the request to an upstream DNS resolver (e.g., `1.1.1.1`).
3. Returns the valid response to the client.

## 🚀 Getting Started

### Prerequisites

* Java 21 or later
* Build tool like Maven or Gradle (suggested)

### Building from Source

```bash
git clone https://github.com/AsterExcrisys/advertisement_blocker.git
cd advertisement_blocker
javac -d out src/**/*.java
```

Or use a build tool:

**Maven:**

```bash
mvn clean package
```

### Running the Proxy

```bash
java -jar advertisement_blocker.jar
```

You can also specify options:

```bash
java -jar advertisement_blocker.jar ./path/to/name/servers.txt ./path/to/filtered/domains.txt --server-port=5353
```

### Configuration Options

| **Index/Option**             | **Name(s)**                      | **Description**                                                                         | **Default** |
|------------------------------|----------------------------------|-----------------------------------------------------------------------------------------|-------------|
| `0`                          | *nameServers*                    | The path to the file containing a list of DNS name servers (resolvers), one per line.   | *Required*  |
| `1`                          | *filteredDomains*                | The path to the file containing a list of filtered domains, one per line.               | *Required*  |
| `-wl`, `--is-whitelist`      | *isWhitelist*                    | Flag to indicate if the proxy should use a **whitelist** (instead of blacklist).        | `false`     |
| `-wc`, `--is-wildcard`       | *isWildcard*                     | Flag to indicate if the proxy should use **wildcard** matching.                         | `false`     |
| `-p`, `--server-port`        | *serverPort*                     | The port on which the server will listen for requests.                                  | `53`        |
| `-c`, `--cache-limit`        | *cacheLimit*                     | Maximum number of DNS responses stored in the cache.                                    | `1000`      |
| `-r`, `--requests-limit`     | *requestsLimit*                  | Max number of requests each handler thread should process.                              | `100`       |
| `-min`, `--minimum-threads`  | *minimumThreads*                 | Minimum number of handler threads maintained at all times.                              | `1`         |
| `-max`, `--maximum-threads`  | *maximumThreads*                 | Maximum number of handler threads that can exist.                                       | `10`        |

## 📄 Name Servers Format

Plaintext file, one domain/address per line:

```
1.1.1.1
1.0.0.1
8.8.8.8
8.0.0.8
```

## 📄 Filtered Domains Format

Plaintext file, one domain per line:

```
ads.service.com
tracker.domain.net
doubleclick.net
```

## 🧪 Testing

You can test it using `dig`:

```bash
dig @localhost -p 5353 example.com
```

If blocked, you'll get a `BLOCKED` response.

## 🔒 Security Notes

* Should be run with appropriate permissions (port 53 requires root on UNIX).
* Not recommended for production use without proper sandboxing or validation.

## 📜 License

GNU GPLv3 License. See [LICENSE](LICENSE) for more information.

## 🙌 Credits

Inspired by Pi-hole, AdGuard, and other DNS filtering solutions.
