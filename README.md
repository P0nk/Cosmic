# Cosmic - MapleStory v83 server
Development Branch
## Introduction
Cosmic launched as a successor to HeavenMS on March 21st 2021.

HeavenMS is archived, ie. it receives no further updates. This project aims to continue its development; mainly by improving code quality and make getting into PS development as easy as possible.

This is an open source project. Anyone may contribute by opening a pull request.

Only the server side is maintained. The client is directly copied from HeavenMS.


Beware - ***This server emulator is not production ready.***
It can be useful for testing things locally or for trying out ideas, but launching a new private server based on this and opening it up to the public
without knowing what you're doing is not recommended.


## Development information

### Status (updated 2022-10-16)

Development is currently <span style="color:Yellow">**sporadic**</span>. 

My time is very limited nowadays, but I try to keep up with the submitted pull requests. I may submit some stuff of my own, once in a while.

### Ways to contribute

* Submit a Pull Request (fork -> commit -> PR). If you don't know where to start, have a look at the issues on GitHub.
* Report a bug (preferably as an Issue on GitHub, as reports on Discord may be forgotten or lost)
* Spread the word about Cosmic

### Working with GitHub

Anyone with a GitHub account can contribute by making some changes in a branch and opening up a PR. 

All activity on the GitHub repo (opening PR, commenting, creating issue, etc.) is automatically pushed (via webhook) to a public Discord channel for visibility.

Issues is the main place where bugs, issues or general improvements are tracked. Feel free to submit a new issue, but please keep it in English. By providing a good description, you increase the chance of a bug being fixed.

Tasks (past, present and future) are kept in the Cosmic project, which you get to via the "Projects" tab. This gives you an idea of where the project is moving.

### Versioning

The project follows the [SemVer](https://semver.org/) versioning scheme using git tags. 
As a pull request gets merged, a new version is automatically created.

Bug fixes result in bumped patch version: 1.2.__3__ -> 1.2.__4__

General improvements result in bumped minor version: 1.__2__.3 -> 1.__3__.3

Major changes result in bumped major version: __1__.2.3 -> __2__.2.3

### Cosmic

- GitHub: https://github.com/P0nk/Cosmic
- Discord: https://discord.gg/JU5aQapVZK

### HeavenMS
- GitHub: https://github.com/ronancpl/HeavenMS
- Discord: https://discord.gg/Q7wKxHX

## Tools / downloads
* **Java 17 SDK** - Needed to compile and run Java code. Install manually or through IntelliJ depending on how you prefer to launch the server. Not required for launching with Docker.
  * Link: https://jdk.java.net/17/
	

* **IntelliJ IDEA** - Java IDE and your main tool for working with the source code. Community edition is good enough.
  * Link: https://www.jetbrains.com/idea/
	

* **MySQL Community Server 8** - Database for game data.  
  * Link: https://dev.mysql.com/downloads/mysql/
	

* **MySQL Workbench 8** - Client for interacting with the database. Other clients do exist. 
  * Link: https://dev.mysql.com/downloads/workbench/
	

* **Docker Desktop** (optional) - For launching the game locally with less hassle.  
  * Link: https://www.docker.com/products/docker-desktop
	

* **Client files and general tools**
  * Link: https://drive.google.com/drive/folders/1hgnb92MGL6xqEp9szEMBh0K9pSJcJ6IT?usp=sharing
  * This is Ponk's own Google Drive, similar to how Ronan provides files for HeavenMS.
	

### MapleStory client 

- Latest localhost client: https://hostr.co/amuX5SLeeVZx

**Important note about localhost clients**: these executables are red-flagged by antivirus tools as __potentially malicious software__,
this happens due to the reverse engineering methods that were applied onto these software artifacts. 
Those depicted here have been put to use for years already and posed no harm so far, so they are soundly assumed to be safe.


## Getting started
The localhost MapleStory client needs to be installed, as well as the server that will host the game.

### Installing the client

1. Install MapleStory with "MapleGlobal-v83-setup.exe" in your folder of choice (e.g. "C:\Nexon\MapleStory") and follow their instructions.
2. Once done, erase these files: "HShield" (folder), "ASPLnchr.exe", "MapleStory.exe" and "Patcher.exe".
3. Extract into the client folder the "HeavenMS-localhost-WINDOW.exe" (from now on referred to as "localhost.exe") from the provided link.
4. Overwrite the original WZ files with the ones provided on the Google Drive: "CosmicWZ-v1-2021.05.10.zip"
	- This is currently identical to the latest HeavenMS WZ files (except for the file name): "commit397_wz-20210321T173600Z-001.zip"

#### Editing localhost IP target

If you are not using "localhost" as the target IP on the server's config file, you will need to HEX-EDIT localhost.exe to fetch your IP. Track down all IP locations by searching for "Type: String" "127.0.0.1", and applying the changes wherever it fits.

To hex-edit, install the Neo Hex Editor from "free-hex-editor-neo.exe" and follow their instructions. Once done, open localhost.exe for editing and overwrite the IP values under the 3 addresses. Save the changes and exit the editor.

(TODO: find suitable alternative to Neo Hex Editor)

#### Testing the localhost

Open the "localhost.exe" client. 
If by any means the program did not open, and checking the server log your ping has been listened by the server 
and you are using Windows 8, 10 or 11, it is probably some compatibility issue.

In some cases it helps to spam click the exe a few times (2-3 times usually works for me on W10).

In that case, extract "lolwut.exe" from "lolwut-v0.01.rar" and place it on the MapleStory client folder ("C:\Nexon\MapleStory"). 
Your "localhost.exe" property settings must follow these:

Note: "lolwut.exe" is currently not available in the Google Drive.

* Run in compatibility mode: Windows 7;
* Unchecked reduced color mode;
* 640 x 480 resolution;
* Unchecked disable display on high DPI settings;
* Run as an administrator;
* Opening "lolwut.exe", use Fraysa's method.

Important: should the client be refused a connection to the game server, it may be because of firewall issues. Head to the end of this file to proceed in allowing this connection through the computer's firewall. Alternatively, one can deactivate the firewall and try opening the client again.
You can also search the server logs (/logs/cosmic-log.log) if any connection attempts have been made to ease debugging.

---
### Installing the server 
1. Configure the project
2. Set up the database
3. Launch the server

If you are using Docker (quick start):
1. Configure the project
2. Launch the server

#### Configuring the project

The easiest way to set up your project is to clone the repository directly into a new IntelliJ project.

1. Install IntelliJ
2. Create a new "Project from Version Control..."
3. Enter the URL to this GitHub repository: "https://github.com/P0nk/Cosmic.git"
4. Click on "Clone". A new project will now be created with all the files from the repository.

#### Setting up the database

1. Install MySQL Server 8 and MySQL Workbench 8.  
2. Using Workbench, create a new user with username "cosmic_server" and password "snailshell". 
   This the default configuration in Cosmic.
   * (Optional) Restrict the Schema Privileges for this new user for improved security. 
	 Add a new entry with "Schemas matching pattern: cosmic" and only select "SELECT", "INSERT", "UPDATE", "DELETE" under "Object Rights"
3. Run the sql scripts in the "database/sql" directory of the project in the order indicated by their names. 
	* Make sure you are connected to the database with the "root" user to be able to run the scripts.
	* Run scripts one by one through the menu: "File" -> "Run SQL Script" -> select the script file to run -> "Run"
	* The 3rd script "3-db_shopupdate" is optional. It adds custom shop items for certain NPCs.
    * The 4th script "4-db_admin" is also optional, but recommended if you are new. It adds an admin account to simplify the setup.

Use this info when you connect to MySQL Server for the first time:
* Server Host: localhost
* Port: 3306
* Username: root
* Password: <whatever password you set during MySQL Server installation>

At the end of the execution of these sql scripts, you should have installed a database schema named "cosmic". 
REGISTER YOUR FIRST ACCOUNT to be used in-game by **manually creating** an entry in the table "accounts" in the database with a username and password.


### Running the server

Configure the IP you want to use for your MapleStory server in "config.yaml" file, or set it as "localhost" if you want to run it only on your machine.
Alternatively, you can use the IP given by Hamachi to use on a Hamachi network, or you can use a non-Hamachi method of port-forwarding. Neither will be approached here.


To launch the server, you may either:
* Launch inside IntelliJ
* Launch a built jar file
* Launch with Docker

#### Run inside IntelliJ
1. Open the file src/main/java/net/server/Server.java.
2. Click the green arrow to the left of the class definition "public class Server", and then "Run Cosmic". 
   * Alternatively (recommended), create a new Configuration that points to "net.server.Server".
3. The server launches in a terminal window inside IntelliJ.

#### Run from a jar file
1. Create the jar file
   * The jar file is created by the Maven assembly plugin in the package lifecycle.
   * If you already have Maven installed, simply run the command "mvn clean install" to create the jar file.
   * IntelliJ also comes with built-in Maven support. Open a new terminal window inside IntelliJ, type "mvn clean install" (your command should now be marked green), then Ctrl+Enter to build the jar file.
2. Launch the jar file
   * Double click on "launch.bat" (need to have Java 17 installed)
    
#### Run as containers with Docker
1. Start Docker
2. Run the command "docker compose up" at the root of the project.
    * If you make any changes to the code, make sure you append the "--build" option at the end of the command to force rebuild the server image.

---
### Getting into the game

If you ran the admin sql script, there already exists an account in the database with an admin character on it (GM level 6).

Log in using these credentials:
* Username: "admin"
* Password: "admin"
* Pin: "0000"
* Pic: "000000"

Admin characters have "hide" mode enabled by default. This means your character will be translucent on your screen, and completely invisible to others.
It will also prevent you from controlling mobs (making them stand still). To toggle this mode on and off, type "@hide" in the in-game chat.

By default, the server source is set to allow AUTO-REGISTERING. This means that, by simply typing in a "Login ID" and a "Password", you're able to create a new account.

After creating a character, experiment typing in all-chat "@commands". 
This will display all available commands for the current GM level your character has.

To change a character's GM level, make sure that character is not logged in, then:

1. Open MySQL Workbench;
2. Expand "cosmic" schema;
3. Expand "Tables";
4. Right-click "characters" and click "Select Rows"
5. Find your character in Result Grid. Scroll to the right and find the "gm" column.
6. Edit your character's gm value and click "Apply", and then "Apply" again in the window that appeared, then "Finish".
	* 0 is what ordinary players start with, and 6 is the highest gm value. Higher level gms have access to more commands in game.

---
### Some notes about WZ/WZ.XML EDITING 
Brief introduction to WZ files: they are the asset/data files required by the client and server. The client can read the .wz files directly, but the server requires them in XML format.
The server also does not make use of any of the sprites, which is where different kinds of exporting comes into the picture. HaRepacker allows you to export to Private server XML, which is the .img files packaged in the .wz stripped of sprites and converted to XML.

Link to HaRepacker-resurrected, the standard tool for handling WZ files: https://github.com/lastbattle/Harepacker-resurrected

NOTE: Be extremely wary when using server-side's XMLs data being reimported into the client's WZ, as some means of synchronization between the server and client modules, this action COULD generate some kind of bugs afterwards. Client-to-server data reimporting seems to be fine, though.

#### Editing the v83 WZ's:


* Use the HaRepacker-resurrected 4.2.4 editor, encryption "GMS (old)".
* Open the desired WZ for editing and use the node hierarchy to make the desired changes (copy/pasting nodes may be unreliable in rare scenarios).
* Save the changed WZ, **overwriting the original content** at the client folder.
* Finally, **RE-EXPORT (using the "Private Server..." exporting option) the changed XMLs into the server's WZ.XML files**, overwriting the old contents.

**These steps are IMPORTANT, to maintain synchronization** between the server and client modules.

---
### Portforwarding the SERVER

To use portforward, you will need to have permission to change things on the LAN router. Access your router using the Internet browser. URLs vary accordingly with the manufacturer. To discover it, open the command prompt and type "ipconfig" and search for the "default gateway" field. The IP shown there is the URL needed to access the router. Also, look for the IP given to your machine (aka "IPv4 address" field), which will be the server one. 

The default login/password also varies, so use the link http://www.routerpasswords.com/ as reference. Usually, login as "admin" and password as "password" completes the task well.

Now you have logged in the router system, search for anything related to portforwarding. Should the system prompt you between portforwarding and portriggering, pick the first, it is what we will be using.

Now, it is needed to enable the right ports for the Internet. For Cosmic, it is basically needed to open ports 7575 to 7575 + (number of channels) and port 8484. Create a new custom service which enables that range of ports for the server's channel and opt to use TCP/UDP protocols. Finally, create a custom service now for using port 8484.

Optionally, if you want to host a webpage, portforward the port 80 (the HTTP port) as well.

It is not done yet, sometimes the firewalls will block connections between the LAN and the Internet. To overcome this, it is needed to create some rules for the firewall to permit these connections. Search for the advanced options with firewalls on your computer and, with it open, create two rules (one outbound and one inbound).

These rules must target "one application", "enable connections" and must target your MapleStory client (aka localhost).

After all these steps, the portforwarding process should now be complete.

---

### Client changelog
The following list, in bottom-up chronological order,
holds information regarding all changes that were applied from the starting localhost used in this development.
Some lines have a link attached, that will lead you to a snapshot of the localhost at that version of the artifact.
Naturally, later versions holds all previous changes along with the proposed changes.

**Change log:**

* Fixed Monster Magnet crashing the caster when trying to pull fixed mobs, credits to Shavit. https://gofile.io/?c=BW7dVM (dead link)
* Cleared need for administrator privileges (OS) to play the game, credits to Ubaware.
* Set a higher cap for AP assigning with AP Reset, credits to Ubaware.
* Fixed Monster Magnet crashing the caster when trying to pull bosses. Drawback: Dojo HPBar becomes unavailable. https://hostr.co/SvnSKrGzXhG0
* Fixed some 'rn' problems with quest icons & removed "tab" from party leader changed message. https://hostr.co/tsYsQzzV6xT0
* Removed block on applying attack-based strengthening gems on non-weapon equipments. https://hostr.co/m2bVtnizCtmD
* Set a higher cap for SPEED.
* Removed the AP assigning block for beginners below level 10. https://hostr.co/AHAHzneCti9B
* Removed block on party for beginners level 10 or below. https://hostr.co/JZq53mMtToCz
* Removed block on MTS entering in some maps, rendering the buyback option available.
* Removed "AP excess" popup and limited actions on Admin/MWLB, credits to kevintjuh93.
* Removed "You've gained a level!" popup, credits to PrinceReborn.
* Removed caps for WATK, WDEF, MDEF, ACC, AVOID.
* 'n' problem fixed.
* Fraysa's https://hostr.co/gJbLZITRVHmv
* Eric's MapleSilver starting on window-mode.