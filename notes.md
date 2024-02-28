# Design

## Philosophy

Enjure revolves around a few core concepts:

- Easy to get going, and easy to keep going.
- Less components means less complexity.
- Convention over configuration.
- Real value over re-packaging.
- Your opinion, your library.
- A holistic approach.

### Easy to get going, and easy to keep going

Clojure is notoriously difficult to pick up for new people. Enjure will have a tight toolchain to
address the short fall. The Enjure cli will create projects, controllers, views, entities, and data. It
should be easy to get a new project off the ground as well as being useful enough for experienced
Clojure developers.

Keeping a project going isn't just limited to toolchains though. A production system has a ton of moving
parts. A good architecture should be able to grow with the increase in requirements. Enjure will aim to
grow over time with principled design.

### Less components means less complexity

It seems entropy permeates even the career of the humble software developer. Software development culture
favours ever increasing complex architecture, yet it is the simple architectures that last. Every increment
in software architecture (infrastructure and software) components creates an exponential increase in
complexity. Enjure must use as few components as possible to achieve its goals to offer a robust solution
for developers.

### Convention over configuration

Most Clojure systems in production are identical ring applications with PostgreSQL as their database,
differing only in their minutia and configuration. Misconfiguration of systems creates the most incidents
of any possible system faults. It's important to limit the amount of configuration by developers and
operators.

### Real value over re-packaging

Past attempts at this kind of project always seem to miss the mark. Often, they obsess about "not getting
in the way". This leads to a problem where  people are afraid to innovate in the space. What usually happens
is people promise not to get in the way and end up re-creating and shipping the same bag of libraries.
More recently there's been obession with managing system components (infrastructure) in the newer Clojure
frameworks, but they still lack the overall software design that makes a framework good.

### Your opinion, your library

You are responsible for what you create, including software. If you insist on developing software that
"won't get in the way", then please go write that software. What I don't think a lot of Clojure developers
realize is that their favourite libraries are incredibly opinionated. Usually, some master craftsperson
has code-smithed that beautiful library being used and taken into account the developer experience
of the API, appearing "unopinionated". One need only to click the `src` directory.

Oddly, there seems to be a certain hesitation to work with anything opinionated in Clojure. Of course
developers are free to do whatever they want, but this doesn't work so well in pracitice. Unfortunately,
trying to get a pull request merged at a Clojure shop becomes a task in itself since code review tends
to turn into a battle of opinions, creating a stalement.

Inversion of control can eliminate these redundant discussions on top of eliminating a ton of technical
decision-making and general faffery. Take it or leave it, but some of us want to get back to shipping.

### A holistic approach

Contemporary software development favours incremental architectural changes, yet it often leaves behind
the holistic approach. The process creates a cyclomatically complex nightmare in Clojure codebases. Oft
forgetting about other components that may be affected by the changes introduced. For example, Clojure
codebases introduce database entities without considering that CRUD controllers and views could
easily be created automatically with the right interface and utilities.

A holistic approach would consider the potential impact of all components, specially the interoperability
of those components.

## Commond Line Interface (CLI)

Since the Enjure CLI will be the first experience many developers will have with enjure, it should be
rigidly consistent, to the point where users can just guess the commands. Useful errors and output
as well. Similary to the rails cli maybe, or django?

- enjure generate <thing> <name> where thing could be one of migration, controller, view, etc.\
- enjure destroy <thing> <name>
- enjure migrate
- enjure notes ; Lists all NOTE, FIXME, HACK, TODO, and nb; comments.

## Data Model

I've been experimenting with something called the ReactiveRecord. I kind of want an in-memory representation
of the database similar to datomic, but without all the hoopla, but doing this for Postgres feels an awful
lot like creating a write-through cache. A nightmare for consistency unless I use software transactional
memory (STM). I'm worried that might be slow though. Would the useabelity aspect be a worthy trade-off? How
do users write and create queries/entities?

Ideally, there would be some kind of common interface between entities, so the CRUD routes for some of them
could be automatically generated like in Django, same with the Admin console.

I'm thinking of using the map type as the interface for teh ReactiveRecord since next.jdbc queries often
return Clojure maps anyways. Maybe map <-> internal ref. I'd probably have to wrap next.jdbc in some
wonky ReactiveRecord stuff.

Another angle of this I'm not thinking of is creating and running database migrations. Obviously I'd want
the templates created with the enjure cli. I like this idea of auto-magically being able to rollback
a migration. I'm not sure how that works internall, I'm guessing the version the migrations in a table.
I have no idea what an entity DSL would look like. I'm not sure if I just want to have a map/keyword
based DSL, that feels too configuration based, but perhaps not.

``` clojure
  ;; defentity does two things, it interperts the DSL into SQL DDL.
  ;; and creates a type var for the ReactiveRecord to query the
  ;; entity rows. It won't run the SQL DDL if the entity already
  ;; has a table in the database. That's what defmigration is for.

  ;; The map concept feels too similar to configuration based options
  ;; that already exist. It's error prone and has lots of magic.
  (defentity users
    {:name e/string
     :email (-> e/string e/unqiue e/not-null e/index)
     :password (-> e/string e/not-null)
     :is-admin (-> e/boolean e/not-null (e/default false))})

  ;; For some reason the Clojure DSL option feels nice.
  ;; + Feels good
  ;; - At times could be verbose like Lisps of yester generation
  (defentity users
    (create-table
     (when-not (exists? users)
       (column name e/string)
       (column email (-> e/string e/unique e/not-null))
       (column password (-> e/string e/not-null))
       (column is-admin (-> e/boolean e/not-nul (e/default false)))))

    (create-index
     (e/unique
      (when-not (exists? users-idx)
        (e/on (:name users))))))

  ;; Hybrid approach.
  ;; + Feels very close to writing real-world Clojure.
  ;; - Relies on both parsing parens and a map.
  (defentity users
    (create-table
     (when-not (exists? users)
       {:name e/string
        :email (-> e/string e/unqiue e/not-null)
        :password (-> e/string e/not-null)
        :is-admin (-> e/boolean e/not-null (e/default false))}))

    (create-index
     (e/unique
      (when-not (exists? users-idx) ;;where does users-idx come from?
        (e/on (:name users))))))

  (defmigration fix-users-name
    (alter-table
     users
     (when (exists? (:name users))
       {:name (-> e/string e/not-null)})))

  ;; Ideally, migration above would be syntactic sugar for:
  (defmigration fix-users-name
    (up
     (alter-table
      users
      (when (exists? (:name users))
        {:name (-> e/string e/not-null)})))
    (down
     (alter-table
      users
      (when (exists? (:name users))
        {:name e/string}))))
```

## Views

``` clojure
;; defview always returns content-type text/html
;; something like this should render index.html
;; and return the value. However, enjure internals
;; will manage things like anti-forgery tokens
;; in conjuction with defcontroller for an
;; opaque experience.
(defview index-view "/"
  [request]
  (render index.html))

(defxmlview sitemap-view "/sitemap.xml"
  [request]
  (clojure.xml/xml enjure.core.routes))
```

## Controllers

Not sure if I should allow controllers to be place anywhere in the repo. I feel like it
might go against inversion of control. Controllers just live in ./controllers.

It's also not clear if load-file compiles the files to byte code, or just evaluates the
forms. The latter would not be great for performance.

Turns out it's compiled.

I've been thinking about a Rails style mini-dsl where the routes are baked into the
conrollers names since routes and their corresponding handlers rarely change. I do like
the decoupling that registering a route handler gives the developers, but it's so
fucking annoying to set up a router, with all the middleware and coercion BS and then have
a handler thrown into the mix somewhere. Should be just write controller function, here's
route, now work. No fuss. No configuration.

Perhaps a macro, instead of a DSL. something like

``` clojure
;; controllers for index post/put/delete
(defcontroller signin-controller "/signin"
  [req]

  (post
    (let [{:keys [email password]} (:params req)]
      (try
        (if-let [user-id (:id (check-email-and-password email password))]
          (-> (redirect "/")
              (assoc :session {:user-id user-id
                               :recreate true}))
          (-> (bad-request (bad-signin-page))
              (content-type "text/html")))
        (catch Exception e
          (log/errorf (str "Caught exception in signin-handler: %s\n"
                           "email: %s \n")
                      (.getMessage e)
                      email)
          (internal-server-error)))))

  (put)

  (delete))
```

fuck I just realized the controllers will probably get compiled regardless. What I probably
need is to find out which namespaces have "controller" in them. project.controller.<name>
maybe.

Why am I even loading these namespaces or files anyways. Wouldn't a macro suffice? to prevent controllers
from living outside of the "controllers" folder ?

What about REPL integration? It might be useful to define these controllers or views and then able to
see the output. For example, the above controller could intern a namespace and allow for access

``` clojure
user=> (signin-controller/post (mock-request))
```

## Distibuted Task Manager / async workers

Just learned that there's no real distributed task management library for Clojure. So there's another bit
of totally new stuff to create for Enjure. Sure there's a rabbitmq library, but it doesn't manage tasks,
persistence, replication, acknowledgements or redelivery, HA, backups or monitoring.

So I should pick a message broker / buffer and re-create celery or sidekiq. Maybe spin it off as a separate
library. If I keep it strictly bundled with Enjure, people that need it might be more inclined to adopt
Enjure.

Ideally the APi would just have a factory function create a job type from a function. The redis library
has a message queue built into it, but lacks any kind of task or queue API (on purpose).

``` clojure
(dispatch-worker (->worker (fn worker [] (println "hello distibuted"))))

(-> some-work-fn
    (->worker {:retry-strategy :exponential-decay :priority 1})
    dispatch-worker)

```


## Monitoring



## Scalability
