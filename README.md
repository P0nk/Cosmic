# Cosmic
Cosmic is a server emulator for Global MapleStory (GMS) version 83.

## Introduction

Cosmic launched on March 2021. It is based on code from a long line of server emulators spanning over a decade - starting with OdinMS (2008) and ending with HeavenMS (2019).

This is mainly a Java based project, but there are also a bunch of scripts written in JavaScript.

Head developer and maintainer: __Ponk__.\
Contributors: a lot of people over the years, and hopefully more to come. Big thanks to everyone who has contributed so far!

Join the Discord server where most of the discussions take place: https://discord.gg/JU5aQapVZK

### Goals
What we are working towards.
* __Vanilla gameplay__ - stay as close to the original game as possible (within reason).
* __Ease of use__ - getting started should be frictionless and contributing to the project straightforward.
* __Reduce technical debt__ - making changes should be easy without causing unintended side effects.
* __Modern tools & technologies__ - stay appealing by continuously improving the code and the project as a whole. 

### Non-goals
Explicitly excluded from the scope of the project.
* __Custom gameplay features__ - existing custom features will be removed over time and new ones are unlikely to be added.
* __Client development__ - this project is focused on the server. Please go elsewhere for client related questions.
* __Public server__ - there will not be an official Cosmic server open to the public. Feel free to launch your own server __at your own risk__. No support will be provided.

## Project setup

### Contribute
You may contribute to the project in various ways, mainly through GitHub:
* Providing improvements to the code through a [Pull Request](https://github.com/P0nk/Cosmic/pulls) from your own fork. 
* Reporting a bug by creating an [Issue](https://github.com/P0nk/Cosmic/issues).
* Providing information to existing issues or reviewing pull requests that others have made.
* ...and in other ways that I haven't thought of!

### Continuous integration
A GitHub Actions pipeline is set up to run the build automatically when a new pull request is opened or commits are pushed to an existing one. This ensures that the code compiles and all the tests pass.

Once a pull request is merged, a tag with the new version is automatically created.

### Discord integration
Most GitHub activity is pushed to a Discord channel for visibility. This works by leveraging a webhook. The activity includes (but is not limited to): merged commits, created PRs, comments, and new tags.

### Versioning
The project follows the [semantic versioning](https://semver.org/) scheme using git tags.
* *Bug fixes* are treated as PATCH: 1.2.__3__ -> 1.2.__4__
* *General changes or improvements* are treated as MINOR: 1.__2__.3 -> 1.__3.0__
* *Major changes* are treated as MAJOR: __1__.2.3 -> __2.0.0__

## Quick start

### Prerequisite

1.  [Docker](https://www.docker.com)
2.  [Docker Compose](https://docs.docker.com/compose/install)

### Start services

To start all services, run:

```bash
docker compose up -d
```

Then you can use

```bash
docker compose logs -f maplestory
```

to check the logs of Cosmic server.

Once the server is ready, you will see a message like:

```log
07:24:20.269 [main] INFO  server.Server - Cosmic is now online after 14547 ms.
```

### Stop services

```bash
docker compose down
```

### Rebuild

You must rebuild images after any code changes:

```bash
docker compose build
```

## Development

This project provides [Dev Container](https://code.visualstudio.com/docs/devcontainers/containers)
that make setting up the development environment a breeze.
You can check the settings in the [.devcontainer](./.devcontainer) folder.

## Local setup
You can also run Cosmic on your actual machine.

We will set up the following:
- Database - the database is used by the server to store game data such as accounts, characters and inventory items.
- Server - the server is the "brain" and routes network traffic between the clients.

### 1 - Database
You will start by installing the database server and database client. Then you will connect to the server with the client to create a new database schema.

#### Steps

1. Download and install [MySQL Community Server 8+](https://dev.mysql.com/downloads/mysql/). You will have to set a root password. Make sure you don't lose it because you will need it later.
2. Download and install [HeidiSQL](https://www.heidisql.com/download.php).
3. Connect to the database: 
   1. Open HeidiSQL
   2. Create a new Session: "New" -> fill in your password -> "Save"
   3. Connect to the database: click on your saved session -> "Open"
4. Create a new database schema:
   1. In the opened session, right-click on the session name in the menu on the left
   2. "Create new" -> "Database" -> database name should be "cosmic" -> "OK"
5. Done. The database is now ready. Once the Cosmic server starts, it will create tables and populate some of them with initial data.

### 2 - Server
You will start by cloning the repository, then configure the database properties and lastly start the server.

#### Prerequisites
* Java 21 (I recommend [Amazon Corretto](https://aws.amazon.com/corretto))
* IDE (I recommend [IntelliJ IDEA](https://www.jetbrains.com/idea/))

#### Steps

1. Clone Cosmic into a new project. In IntelliJ, you would create a new project from version control.
2. Open _config.yaml_. Find "DB_PASS" and set it to your database root user password.
3. Start the server. The main method is located in `net.server.Server`.
4. If you see "Cosmic is now online" in the console, it means the server is online and ready to serve traffic. Yay!

Below, I list other ways of running the server which are completely optional.

#### Jar
Another option is to start the server from a terminal by running a jar file. You first need to build the jar file from source which requires [Maven](https://maven.apache.org/). Fortunately, [Maven Wrapper](https://maven.apache.org/wrapper/) is provided so you don't have to install Maven separately.

Building the jar file is as easy as running ``./mvnw.cmd clean package``. The project is configured to produce a "fat" jar which contains all dependencies (by utilizing the _maven-assembly-plugin_). Note that the WZ XML files are __not__ included in the jar.

To run the jar, a ``launch.bat`` file is provided for convenience. Simply double-click it and the server will start in a new terminal window. 

Alternatively, run the jar file from the terminal. Just remember to provide the `wz-path` system property pointing to your wz directory.

## Client
Client is the application used to _play the game_, i.e. MapleStory.exe.

The files are located in a separate repository: https://github.com/P0nk/Cosmic-client

Follow the installation guide in the README.

## Getting into the game
You have successfully started the client, and you're looking at the login screen.

### Logging in
At this point, you can log in to the admin account using the following credentials:
* Username: "admin"
* Password: "admin"
* Pin: "0000"
* Pic: "000000"

You can also create a new regular account by typing in your desired username & password and attempting to log in. This "automatic registration" feature lets you create new accounts to play around with. It is enabled by default (see _config.yaml_).

### Entering the game
Create a new character as you normally would, and then select it to enter the game. Hooray, finally we're in!

If you log in to the "Admin" character, you'll notice that the character looks almost invisible. This is hide mode, which is enabled by default when you log in to a GM character. You won't be visible to normal players and no mobs will move if you're alone on the map. Toggle hide mode on or off by typing "@hide" in the in-game chat.

Hide is one of many commands available to players, type "@commands" to see the full list. Higher ranked GMs have access to more powerful commands.

That's it, have fun playing around in game! 

## Advanced concepts
Some slightly more advanced concepts that might be useful once you're up and running.

### Host on remote server
You don't have to host the server on your local machine to play. It's possible to host on a remote server such as a VPS or a dedicated server.

I leave it to you to figure out the server hosting part, but once you have that running you'll need to edit the client ip to point to your remote server ip.

### WZ files
WZ files are the asset/data files required by the client and server. Typically, the [HaRepacker-resurrected](https://github.com/lastbattle/Harepacker-resurrected) tool is used to manage (view, edit, export) the .wz files.

The client can read the .wz files directly, but the server requires them to be in XML format. The server does not make use of the sprites, which is the motivation for different kinds of exporting. 
HaRepacker allows you to export to "Private server", which is the .img files packaged in the .wz stripped of sprites and converted to XML. This takes much less disk space.

This server requires custom .wz files (unfortunately), as you may have noted during installation of the client. The intention is for these to be removed eventually and to solely run on vanilla .wz files.

#### WZ editing
* Use the HaRepacker-resurrected editor, encryption "GMS (old)".
* Open the desired .wz for editing and use the node hierarchy to make the desired changes (copy/pasting nodes may be unreliable in rare scenarios).
* Save the changed .wz, overwriting the original content at the client folder.
* Finally, re-export (using the "Private Server" exporting option) the changed XMLs into the server's .wz XML files (found in the "wz" directory), overwriting the old contents.

Make sure to always export from the client .wz files to the server XML, and not the other way around. 

Editing the client .wz without exporting to the server may lead to strange behavior.
