# realworld-clj

generated using Luminus version "4.16"

`lein new luminus realworld-clj +jetty +crux +service +auth`


## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Getting started developing (using OSX)

Assuming that Brew is installed and using VS Code

1. Install leiningen (`brew install leiningen`)
2. Install Calva addon for VS Code
3. Open folder in VSCode
4. Run command "Calva: Start a Project REPL and Connect". Select "leiningen" profile
5. REPL should be running! Type `(start)` to boot up the web server.

To start making changes to the code, go to the "src/clj/realworld_clj/core.clj" file, which is the main entrypoint in the application.

## Running

To start a web server for the application, run:

    lein run 

## License

Copyright © 2021 FIXME
