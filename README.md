# Advertisement Blocker

A simple, configurable DNS proxy server written in Java that blocks known ad-serving domains. This tool intercepts DNS requests and filters out domains from a blocklist, preventing clients from resolving and connecting to advertisement networks and trackers.

## ✨ Features

* ✅ Intercepts and handles DNS requests (must be set as the upstream resolver)
* 🚫 Blocks known advertisement and tracking domains
* 📝 Supports both blacklists and whitelists with exact or wildcard matchers
* 💾 Lightweight, few external dependencies
* 🔧 Configurable via CLI arguments (uses simple TXT files for name servers and filtered domains)
* 📄 Logs DNS queries and blocked domains

## 📦 How It Works

1. The proxy listens for DNS requests from clients (either through UDP or TCP).
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
java -jar advertisement_blocker.jar ./path/to/name/servers.txt ./path/to/filtered/domains.txt --server-port=53000
```

### Configuration Options

| **Index/Option**           | **Name(s)**       | **Description**                                                                       | **Default** |
|----------------------------|-------------------|---------------------------------------------------------------------------------------|-------------|
| `0`                        | *nameServers*     | The path to the file containing a list of DNS name servers (resolvers), one per line. | *Required*  |
| `1`                        | *filteredDomains* | The path to the file containing a list of filtered domains, one per line.             | *Required*  |
| `-sm`, `--server-mode`     | *serverMode*      | The mode of the server to use for receiving requests.                                 | `UDP`       |
| `-sp`, `--server-port`     | *serverPort*      | The port on which the server will listen for requests.                                | `53`        |
| `-sr`, `--should-retry`    | *shouldRetry*     | Flag to indicate if the proxy should use a retry mechanism with failed requests.      | `false`     |
| `-wl`, `--is-whitelist`    | *isWhitelist*     | Flag to indicate if the proxy should use a **whitelist** (instead of blacklist).      | `false`     |
| `-wc`, `--is-wildcard`     | *isWildcard*      | Flag to indicate if the proxy should use **wildcard** matching.                       | `false`     |
| `-cl`, `--cache-limit`     | *cacheLimit*      | Maximum number of DNS responses stored in the cache.                                  | `1000`      |
| `-rt`, `--request-timeout` | *requestTimeout*  | Timeout for each incoming request to gain access to its resolver (milliseconds).      | `5000`      |
| `-rl`, `--requests-limit`  | *requestsLimit*   | Maximum number of requests each handler task should process.                          | `100`       |
| `-mnt`, `--minimum-tasks`  | *minimumTasks*    | Minimum number of handler tasks maintained at all times.                              | `5`         |
| `-mxt`, `--maximum-tasks`  | *maximumTasks*    | Maximum number of handler tasks that can exist.                                       | `10`        |

## 📄 Supported Resolver Types

There are currently 5 resolver types supported by this proxy. Be aware that this only refers to **upstream** resolver types. The only types support for **downstream** requests are DNS over UDP and TCP.

| **Full Name**         | **Short Name** | **Status**                     | **Format**                             |
|-----------------------|----------------|--------------------------------|----------------------------------------|
| `DNS (over UDP)`      | *STD*          | Supported                      | Type:Address:(Protocol)                |
| `DNS (over TCP)`      | *STD*          | Supported                      | Type:Address:(Protocol)                |
| `DNSSEC (over UDP)`   | *SEC*          | Supported                      | Type:Address:(Trust-Anchor):(Protocol) |
| `DNSSEC (over TCP)`   | *SEC*          | Supported                      | Type:Address:(Trust-Anchor):(Protocol) |
| `DNSCrypt (over UDP)` | *N/A*          | Not Supported                  | N/A                                    |
| `DNSCrypt (over TCP)` | *N/A*          | Not Supported                  | N/A                                    |
| `DNS over TLS`        | *DOT*          | Supported                      | Type:Address:(Port)                    |
| `DNS over DTLS`       | *DOD*          | Not Supported                  | Type:Address:(Port)                    |
| `DNS over QUIC`       | *DOQ*          | Supported (Needs to be tested) | Type:Address:(Port)                    |
| `DNS over HTTPS`      | *DOH*          | Supported                      | Type:Address:(Endpoint):(Method)       |

## 📄 Name Servers Format

Plaintext file, one domain/address (preceded by the resolver type) per line:

```
STD:1.1.1.1
STD:1.0.0.1
STD:1.1.1.2
STD:1.0.0.2
STD:1.1.1.3
STD:1.0.0.3
STD:8.8.8.8
STD:8.0.0.8
STD:9.9.9.9
```

## 📄 Filtered Domains Format

Plaintext file, one domain per line:

```
ads.service.com
tracker.domain.net
doubleclick.net
ad.doubleclick.net
googlesyndication.com
googleadservices.com
adservice.google.com
ads.google.com
connect.facebook.net
bat.bing.com
```

## 🧪 Testing

You can test it using `dig`:

```bash
dig @localhost -p 53000 example.com
```

If blocked, you'll get a `BLOCKED` response.

## 🔒 Security Notes

* Should be run with appropriate permissions (port 53 requires root on UNIX).
* Only supports DNS over UDP/TCP for downstream requests, other methods (such as DNS over TLS, QUIC, and HTTPS) will simply not work.
* Not recommended for production use without proper sandboxing or validation.

## 📜 License

GNU GPLv3 License. See [LICENSE](LICENSE) for more information.

## 🙌 Credits

Inspired by Pi-hole, AdGuard, and other DNS filtering solutions.
