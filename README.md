# DBHost
Project work for IU by Tibor Péter Szabó.

This project requires Java 8 to run.

The scope of the project was to create a web display, which provides access to data, that is contained in a SQLite3 database. The application uses users to allow or deny access to the commands of the database. The database can be modified using the provided SQLite3 client (sqlite3.exe), that is provided in the /sql subfolder.

To get started with hosting the application, just launch the Start.bat file in the main reprosotory.
After "Waiting for connections..." is displayed, you can connect to the website, by typing "localhost" into the browser.

From this point you have a set of users, into which you can log into, each of them have access to sets of commands, that their access level allows.
Example users and their passwords (username : password):

    admin : admin
    Skylar : Boss
    Tibor : Passw0rd
    BASHlee : Linux
    Rona : 123asd
    LRube : Rulu
    Bekka : Rebi

The user "admin" and "Tibor" have access to all of the commands (even the direct unformated datadumps of the tables).
Skylar as the boss have limited access to commands like "Get Employees","Get machines","Get members of workgroups","Get workgroups".
