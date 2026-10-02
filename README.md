# 🎵 HarmonyPlayer

> A modern desktop music player built with **Java 21 + JavaFX**, featuring local music playback, YouTube audio support, synchronized lyrics, 10-band equalizer, DJ crossfade mode, album artwork, playlists, and multi-device network streaming.

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-blue?style=for-the-badge)
![Maven](https://img.shields.io/badge/Maven-Build-red?style=for-the-badge&logo=apachemaven)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)
![Platform](https://img.shields.io/badge/Platform-Desktop-lightgrey?style=for-the-badge)

---

## 🎧 Overview

**HarmonyPlayer** is a feature-rich desktop music player developed using Java and JavaFX.

The application combines a clean desktop interface with advanced music-player functionality such as:

- 🎵 Local audio playback
- ▶️ Play / Pause / Stop controls
- ⏮️ Previous / Next track navigation
- 🔀 Shuffle mode
- 🔁 Repeat modes
- 🔊 Volume control
- ⏩ Seekable playback
- 🎚️ 10-band audio equalizer
- 🎛️ Equalizer presets
- 🎧 DJ mode with crossfade
- 📺 YouTube audio playback
- 📝 Online lyrics
- 🎤 Synchronized LRC lyrics
- ✏️ Custom lyrics editing
- 💿 Album artwork extraction
- 📋 Playlist management
- 🔎 Playlist search
- 🌐 Multi-device network streaming
- 📱 Connected-device monitoring
- 📡 HTTP audio streaming
- 📊 Song metadata and bitrate information
- 💾 Lyrics caching
- 🎨 Dynamic JavaFX UI and themes

The project is designed as a complete desktop music-player application rather than a simple audio player.

---

# ✨ Features

## 🎵 Music Playback

HarmonyPlayer supports playback of local audio files through JavaFX MediaPlayer.

### Playback controls

- ▶️ Play
- ⏸️ Pause
- ⏹️ Stop
- ⏮️ Previous song
- ⏭️ Next song
- 🔀 Shuffle
- 🔁 Repeat
- ⏩ Seek through songs
- 🔊 Volume adjustment
- Playback progress tracking
- Current playback time
- Total duration display
- Playback state handling

The application automatically reacts when a song finishes and can continue to the next track.

---

# 📂 Local Music Library

Users can add music directly from their computer.

### Supported library actions

- Add individual music files
- Add complete folders
- Remove individual songs
- Clear playlist
- Search songs
- Read audio metadata automatically

When a local audio file is imported, HarmonyPlayer attempts to read:

- Song title
- Artist
- Album
- Genre
- Year
- Bitrate
- File format
- Album artwork

Metadata extraction is handled using **Jaudiotagger**.

---

# 📋 Playlist Management

HarmonyPlayer includes playlist functionality for organizing music.

### Playlist features

- Create playlists
- Delete playlists
- Switch between playlists
- Default playlist
- Add songs
- Remove songs
- Clear playlist
- Search playlist
- Track current playlist index

Songs can be searched using:

- Title
- Artist
- Album
- Genre

---

# 🔎 Smart Song Search

The playlist panel includes a search field for quickly finding songs.

Search matches are performed against:

```text
Song Title
Artist
Album
Genre
````

This makes it easy to locate tracks even when the playlist contains many songs.

---

# 🎚️ 10-Band Equalizer

HarmonyPlayer includes a configurable **10-band audio equalizer**.

### Frequency bands

```text
32 Hz
64 Hz
125 Hz
250 Hz
500 Hz
1 kHz
2 kHz
4 kHz
8 kHz
16 kHz
```

Each band supports gain adjustment within JavaFX's supported equalizer range.

### Built-in presets

* Flat
* Rock
* Pop
* Jazz
* Classical
* Bass
* Treble
* Vocal

Users can also create custom equalizer presets.

### Equalizer capabilities

* 10-band control
* Preset selection
* Custom band adjustment
* Custom preset creation
* Reset to Flat
* Automatic application to the active MediaPlayer

---

# 🎧 DJ Mode

HarmonyPlayer includes an experimental DJ mode designed for smoother transitions between tracks.

### DJ features

* Automatic crossfade
* Manual crossfade
* Adjustable crossfade duration
* Experimental beat matching
* BPM analysis placeholder
* Playback synchronization support

Crossfade duration can be configured between:

```text
1 – 10 seconds
```

The default crossfade duration is:

```text
3 seconds
```

During a crossfade:

```text
Current Track Volume ↓
        +
Next Track Volume ↑
```

This creates a smoother transition between songs.

---

# 📺 YouTube Audio Support

HarmonyPlayer can add songs using YouTube URLs.

Supported URL formats include:

```text
https://www.youtube.com/watch?v=VIDEO_ID
```

and:

```text
https://youtu.be/VIDEO_ID
```

The application:

1. Extracts the YouTube video ID
2. Retrieves video information
3. Finds an available audio format
4. Downloads the audio
5. Stores it temporarily
6. Plays it through the local JavaFX player

Downloaded files are stored temporarily in:

```text
%TEMP%/HarmonyPlayer
```

or the platform's equivalent temporary directory.

---

# 📝 Lyrics System

HarmonyPlayer includes both normal and synchronized lyrics.

## Normal Lyrics

Lyrics can be retrieved online using:

**lyrics.ovh**

The application searches using:

```text
Artist
+
Song Title
```

Lyrics are cached locally to reduce repeated network requests.

---

# 🎤 Synchronized Lyrics

HarmonyPlayer supports synchronized lyrics using the **LRC format**.

Synchronized lyrics are retrieved through:

**LRCLIB**

The application can:

* Download synchronized lyrics
* Parse LRC timestamps
* Cache LRC files
* Display lyrics synchronized with playback
* Save custom synchronized lyrics

Example LRC structure:

```text
[00:12.50] First lyric line
[00:17.20] Second lyric line
[00:21.80] Third lyric line
```

---

# ✏️ Custom Lyrics

Users can manually edit and save lyrics.

This is useful when:

* Online lyrics are unavailable
* Lyrics contain errors
* Custom lyrics are preferred
* Synchronized lyrics need manual correction

Lyrics are cached locally for future use.

---

# 💿 Album Artwork

HarmonyPlayer attempts to extract album artwork directly from audio-file metadata.

For local songs, artwork is read from the audio file using **Jaudiotagger**.

If artwork cannot be found, the application uses a default album-art placeholder.

Album artwork is displayed in the main player interface.

---

# 🌐 Multi-Device Network Streaming

HarmonyPlayer includes a built-in network streaming system.

A local streaming server can be started directly from the application.

### Network features

* Start streaming server
* Stop streaming server
* Stream current local song
* Pause streaming
* Resume streaming
* Connected-device tracking
* Client count
* Playback position synchronization
* Connection URL
* QR-code URL support

The streaming system uses HTTP-based audio delivery.

---

# 📡 Network Streaming Architecture

The application contains a dedicated network streaming service.

Simplified architecture:

```text
                HarmonyPlayer
                     |
             NetworkStreamController
                     |
              WebStreamService
                     |
          ┌──────────┴──────────┐
          │                     │
     HTTP Server            Playback State
          │                     │
          └──────────┬──────────┘
                     |
              Network Clients
                     |
          ┌──────────┼──────────┐
          │          │          │
        Phone      Tablet      PC
```

The streaming implementation supports HTTP byte-range requests, allowing clients to request portions of the audio file.

---

# 📊 Playback Synchronization

While network streaming is active, HarmonyPlayer tracks playback position.

The server can expose information including:

```text
Streaming status
Connected clients
Current song
Playback position
Server time
```

This allows connected clients to understand the current playback state.

---

# 🎨 User Interface

The application is built using JavaFX and includes a multi-panel desktop layout.

### Main interface

```text
┌──────────────────────────────────────────────────────────────┐
│ File        Tools        Help                                │
├────────────────┬──────────────────────────┬──────────────────┤
│                │                          │                  │
│   PLAYLIST     │       ALBUM ART          │     LYRICS       │
│                │                          │                  │
│ Search...      │      NOW PLAYING         │                  │
│                │                          │                  │
│ Song 1         │      Artist              │   Lyrics         │
│ Song 2         │      Album               │                  │
│ Song 3         │                          │                  │
│                │   ───── Progress ───     │                  │
│                │                          │                  │
│                │  ⏮  ▶  ⏭  🔀  🔁       │                  │
│                │                          │                  │
├────────────────┴──────────────────────────┴──────────────────┤
│ Status                                                       │
└──────────────────────────────────────────────────────────────┘
```

The interface contains:

* Playlist sidebar
* Search field
* Album artwork
* Now-playing information
* Playback controls
* Progress slider
* Volume control
* Lyrics panel
* Status bar
* Tools and application menus

---

# 🎨 Dynamic Theme

The player interface uses JavaFX styling with configurable base and accent colors.

The UI supports a modern dark visual style using:

* Gradient backgrounds
* Transparent panels
* Accent colors
* Shadows
* Custom controls
* Responsive layout
* Styled buttons
* Custom CSS

Main stylesheet:

```text
src/main/resources/styles.css
```

---

# 🧱 Project Architecture

The application follows a layered structure separating:

```text
UI
 ↓
Controllers
 ↓
Services
 ↓
Models
```

This keeps playback, UI, metadata, lyrics, equalizer, DJ mode, and network functionality separated.

---

# 📁 Project Structure

```text
HarmonyPlayer/
│
├── pom.xml
│
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── harmonyplayer/
│       │           │
│       │           ├── HarmonyPlayer.java
│       │           │
│       │           ├── controller/
│       │           │   ├── DJController.java
│       │           │   ├── EqualizerController.java
│       │           │   ├── MainController.java
│       │           │   ├── NetworkStreamController.java
│       │           │   └── PlayerController.java
│       │           │
│       │           ├── model/
│       │           │   ├── Album.java
│       │           │   ├── EqualizerPreset.java
│       │           │   ├── LyricLine.java
│       │           │   ├── Playlist.java
│       │           │   ├── Song.java
│       │           │   └── SynchronizedLyrics.java
│       │           │
│       │           ├── service/
│       │           │   ├── AlbumArtService.java
│       │           │   ├── AudioEffectsService.java
│       │           │   ├── LocalMusicPlayer.java
│       │           │   ├── LyricsService.java
│       │           │   ├── WebStreamService.java
│       │           │   └── YouTubePlayer.java
│       │           │
│       │           └── ui/
│       │               ├── DeviceManagerDialog.java
│       │               ├── DJModeWindow.java
│       │               ├── EqualizerWindow.java
│       │               ├── LyricsEditorDialog.java
│       │               ├── MainWindow.java
│       │               └── SyncLyricsPanel.java
│       │
│       └── resources/
│           ├── styles.css
│           └── icons/
│
└── .vscode/
```

---

# 🛠️ Tech Stack

## Core

| Technology    | Purpose                         |
| ------------- | ------------------------------- |
| Java 21       | Application development         |
| JavaFX 21.0.2 | Desktop UI and media playback   |
| Maven         | Dependency and build management |
| CSS           | JavaFX UI styling               |

## Audio

| Library      | Purpose                          |
| ------------ | -------------------------------- |
| JavaFX Media | Audio playback                   |
| Jaudiotagger | Audio metadata and album artwork |
| JTransforms  | DSP / audio-processing support   |

## Internet / Networking

| Library           | Purpose                         |
| ----------------- | ------------------------------- |
| Apache HttpClient | HTTP requests                   |
| Netty             | Network streaming               |
| Java-WebSocket    | WebSocket support               |
| JmDNS             | Local network service discovery |
| Gson              | JSON parsing                    |

## YouTube

| Library                 | Purpose                                       |
| ----------------------- | --------------------------------------------- |
| Java YouTube Downloader | YouTube video/audio information and downloads |

## Image Processing

| Library               | Purpose              |
| --------------------- | -------------------- |
| TwelveMonkeys ImageIO | Image format support |

## Logging

| Library      | Purpose             |
| ------------ | ------------------- |
| SLF4J Simple | Application logging |

---

# 📦 Maven Dependencies

The project uses Maven for dependency management.

Important dependencies include:

```xml
JavaFX 21.0.2
Gson 2.10.1
Apache HttpClient 4.5.14
Java YouTube Downloader 3.2.3
Jaudiotagger 3.0.1
JTransforms 3.1
Netty 4.1.100.Final
Java-WebSocket 1.5.4
JmDNS 3.6.3
TwelveMonkeys ImageIO 3.10.1
SLF4J Simple 2.0.12
```

---

# 💻 Requirements

Before running the project, install:

### Java

Java Development Kit:

```text
JDK 21+
```

Verify:

```bash
java -version
```

Expected:

```text
java version "21..."
```

### Maven

Install Maven 3.9+.

Verify:

```bash
mvn -version
```

---

# 🚀 Getting Started

## 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/HarmonyPlayer.git
```

Move into the project:

```bash
cd HarmonyPlayer
```

---

## 2. Build the project

```bash
mvn clean package
```

Maven will download the required dependencies and compile the application.

---

## 3. Run the application

Using the JavaFX Maven plugin:

```bash
mvn javafx:run
```

---

# ▶️ Running from an IDE

The project can also be opened using:

* IntelliJ IDEA
* Eclipse
* Visual Studio Code
* NetBeans

Make sure the project is imported as a **Maven project** and JDK 21 is configured.

Main application class:

```text
com.harmonyplayer.HarmonyPlayer
```

---

# 🎵 Adding Music

From the application menu:

```text
File
 ├── Add Local Music
 ├── Add Folder
 ├── Add from YouTube
 ├── Clear Playlist
 └── Exit
```

### Add Local Music

Select one or more audio files from your computer.

### Add Folder

Select a folder containing music.

### Add from YouTube

Provide a YouTube URL and optional title/artist information.

---

# 🎛️ Using the Equalizer

Open:

```text
Tools → Equalizer
```

Available presets:

```text
Flat
Rock
Pop
Jazz
Classical
Bass
Treble
Vocal
```

You can also adjust individual frequency bands and create a custom preset.

---

# 🎧 Using DJ Mode

Open:

```text
Tools → DJ Mode
```

DJ Mode provides:

* Automatic crossfade
* Crossfade duration control
* Experimental beat matching

The default crossfade duration is:

```text
3 seconds
```

---

# 🌐 Using Network Streaming

Open:

```text
Tools → Multi-Device Streaming
```

Start the server and share the generated connection information with devices on the same network.

The streaming system provides:

```text
Connection URL
Connected device count
Connected devices
Current song
Playback position
```

> Network streaming works with local audio files. YouTube tracks are not streamed directly through the network streaming service.

---

# 📝 Lyrics Cache

HarmonyPlayer stores lyrics locally to improve performance and reduce unnecessary network requests.

Default cache location:

```text
~/.harmonyplayer/lyrics/
```

Synchronized lyrics are stored under:

```text
~/.harmonyplayer/lyrics/lrc/
```

The application can clear these caches from:

```text
Tools → Clear Lyrics Cache
```

---

# 🔐 Privacy

HarmonyPlayer is primarily a local desktop application.

Local music files are processed on the user's computer.

Network access is used for features such as:

* Online lyrics
* Synchronized lyrics
* YouTube information/downloads
* Network streaming

No user account is required by the application.

---

# ⚠️ Important Notes

## YouTube

YouTube functionality depends on external services and libraries.

Users are responsible for complying with YouTube's Terms of Service and applicable copyright laws when downloading or using content.

---

## Audio Format Support

Playback compatibility depends on the media formats supported by the installed JavaFX media backend and operating system.

---

## Network Streaming

Network streaming is intended primarily for devices connected to the same local network.

Firewall settings may affect connectivity.

---

# 🧪 Development Status

HarmonyPlayer is an actively developed desktop music-player project.

### Implemented

* [x] Local music playback
* [x] Playlist management
* [x] Song search
* [x] Shuffle
* [x] Repeat
* [x] Volume control
* [x] Seeking
* [x] Metadata extraction
* [x] Album artwork
* [x] YouTube audio support
* [x] Lyrics API
* [x] Lyrics caching
* [x] Synchronized LRC lyrics
* [x] Custom lyrics
* [x] 10-band equalizer
* [x] Equalizer presets
* [x] Custom EQ presets
* [x] DJ crossfade
* [x] Network streaming
* [x] Connected-device tracking
* [x] Playback synchronization
* [x] JavaFX dark UI

### Experimental / Future Improvements

* [ ] Full BPM detection
* [ ] Real beat detection
* [ ] Advanced DSP effects
* [ ] Persistent playlist database
* [ ] Music library auto-scanning
* [ ] Better YouTube metadata extraction
* [ ] Improved album-art web lookup
* [ ] More audio formats
* [ ] Native installers
* [ ] Cross-platform packaging
* [ ] Enhanced mobile streaming interface
* [ ] User-defined keyboard shortcuts

---

# 🧩 Application Architecture

```text
                    ┌─────────────────────┐
                    │     MainWindow      │
                    │      JavaFX UI      │
                    └──────────┬──────────┘
                               │
                    ┌──────────▼──────────┐
                    │   MainController    │
                    └──────────┬──────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
          ▼                    ▼                    ▼
   PlayerController     EqualizerController   DJController
          │                    │                    │
          ▼                    ▼                    ▼
   LocalMusicPlayer     AudioEffectsService   Crossfade Engine
          │
          ├───────────────┐
          │               │
          ▼               ▼
    YouTubePlayer     MediaPlayer
          │
          ▼
     YouTube Audio


        LyricsService
             │
       ┌─────┴─────┐
       ▼           ▼
  lyrics.ovh    LRCLIB
       │           │
       └─────┬─────┘
             ▼
          Cache


 NetworkStreamController
             │
             ▼
      WebStreamService
             │
             ▼
        HTTP Server
             │
       ┌─────┼─────┐
       ▼     ▼     ▼
     PC    Phone  Tablet
```

---

# 📚 Design Patterns & Concepts

The project demonstrates several important software-development concepts.

### MVC-style separation

The application separates:

```text
Model
View/UI
Controller
Service
```

### Event-driven programming

JavaFX callbacks are used for:

* Playback events
* Media readiness
* Song completion
* Time updates
* UI events
* Network status

### Service abstraction

Separate services handle specialized functionality:

```text
LocalMusicPlayer
LyricsService
AlbumArtService
AudioEffectsService
YouTubePlayer
WebStreamService
```

### Asynchronous processing

Background threads are used for operations such as:

* Music importing
* YouTube downloads
* Lyrics retrieval
* Crossfade processing

This helps prevent long-running operations from blocking the JavaFX UI thread.

---

# 🧑‍💻 Development

## Compile

```bash
mvn compile
```

## Run

```bash
mvn javafx:run
```

## Clean

```bash
mvn clean
```

## Package

```bash
mvn package
```

## Clean + Package

```bash
mvn clean package
```

---

# 📦 Build Output

Maven generates build files inside:

```text
target/
```

The project also uses the Maven Shade Plugin to package dependencies into a distributable JAR.

---

# 🐛 Troubleshooting

## JavaFX not found

Make sure JDK 21 is installed and Maven is using the correct JDK.

Check:

```bash
java -version
mvn -version
```

Both should point to Java 21.

---

## Audio does not play

Check:

* File exists
* Audio format is supported
* JavaFX Media backend supports the format
* File is not corrupted
* Correct Java version is being used

---

## Lyrics are not displayed

Check:

* Internet connection
* Artist name
* Song title
* Lyrics provider availability

You can also use the manual lyrics editor.

---

## YouTube playback fails

Check:

* URL format
* Internet connection
* Availability of the video
* YouTube downloader compatibility

---

## Network streaming cannot connect

Check:

* Both devices are on the same network
* Windows Firewall
* Router/client isolation
* Correct connection URL
* Streaming server is running

---

# 🔒 GitHub Recommendations

Before uploading the project, avoid committing generated build files.

Add the following to `.gitignore`:

```gitignore
# Maven
target/
*.class

# IDE
.idea/
*.iml
.vscode/

# OS
.DS_Store
Thumbs.db

# Logs
*.log

# Temporary files
*.tmp
*.temp

# Java
hs_err_pid*

# Application cache
.harmonyplayer/

# YouTube temporary downloads
HarmonyPlayer/
```

> If `HarmonyPlayer/` is your actual project directory, remove that last rule because it would ignore the entire project. It should only be used for a separate temporary-download directory.

---

# 📸 Screenshots

Add screenshots of the application here after uploading them to the repository.

Example:

```markdown
## Screenshots

### Main Player

![HarmonyPlayer Main UI](screenshots/main-player.png)

### Equalizer

![HarmonyPlayer Equalizer](screenshots/equalizer.png)

### DJ Mode

![HarmonyPlayer DJ Mode](screenshots/dj-mode.png)

### Lyrics

![HarmonyPlayer Lyrics](screenshots/lyrics.png)

### Network Streaming

![HarmonyPlayer Network Streaming](screenshots/network-streaming.png)
```

Recommended GitHub structure:

```text
screenshots/
├── main-player.png
├── equalizer.png
├── dj-mode.png
├── lyrics.png
└── network-streaming.png
```

---

# 🗺️ Roadmap

Future development ideas:

```text
[ ] Persistent database
[ ] Automatic music library scanning
[ ] Folder monitoring
[ ] Advanced audio visualization
[ ] Real BPM analysis
[ ] Beat detection
[ ] More DSP effects
[ ] ReplayGain support
[ ] Better playlist persistence
[ ] Smart playlists
[ ] Favorites
[ ] Recently played
[ ] Play history
[ ] Keyboard shortcuts
[ ] Global media keys
[ ] System tray controls
[ ] Native Windows installer
[ ] Linux package
[ ] macOS package
[ ] Improved network player
[ ] Mobile-friendly streaming UI
```

---
