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

enjure follows the typical MVC architecture:

``` shell
$ cd hello-enjure && tree
.
├── deps.edn
├── enjure.edn
└── src
    └── hello_enjure
        ├── controllers
        │   └── signin.clj
        ├── core.clj
        └── pages
            └── index.clj
```

(Model is missing because I haven't implemented it yet)


## Known Issues

- No models / entities / migrations (yet).
- No controllers request coercion for params (yet).
- Router adds 'ghost' path params.
- Router slow AF compared to Reitit.

## Unknown Issues

- Probably plenty, just haven't found them yet.

## License

Copyright © 2023 Janet A. Carr

Distributed under the Eclipse Public License version 1.0.
