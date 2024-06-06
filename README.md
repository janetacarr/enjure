# enjure

Every Clojure Developer's nightmare: An opinionated Clojure web framework.

## Usage

enjure requires the base Clojure install with the command line tools.

You may install the enjure CLI tool by running this:

``` shell
$ git clone https://github.com/janetacarr/enjure.git && cd enjure
$ clj -T:build install-cli
```

You'll possibly be prompted for a password as the enjure CLI requires
root permissions to install. (Supports Linux / Mac OS only right now)

Now, you should be able to move to a directory and create a project
once that finishes installing:

``` shell
$ cd ~
$ enjure help

Usage: enjure <command> <parameters>

Commands:
  serve - Starts the web server
  notes - Print all NOTES, FIXME, HACK, and TODO in project.
  generate - Create a new controller, page, entity, or migration.
  destory - Delete a new controller, page, entity, or migration.
  migrate - Run the database migrations.
  help - Print this message.

$ enjure new hello-enjure && cd hello-enjure
$ enjure serve
```

## Structure

## Known Issues

- The enjure cli tool is slow AF.
- The enjure cli tool silences STDOUT (including errors).

## License

Copyright © 2023 Janet A. Carr

Distributed under the Eclipse Public License version 1.0.
