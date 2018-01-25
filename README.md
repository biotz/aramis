> One for all, all for one  <br>
> -- Alexandre Dumas, The Three Musketeers

# Aramis

This library provides a 
[`Promise.all()`-like](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/all)
capabilities for [re-frame](https://clojars.org/re-frame).

![Demo gif  1](/resources/demo/demo1.gif)

## Usage

Imagine you want to dispatch event `::foo` but only once both 
`::bar` and `::baz` are successfully finished.

#### Add dependency

`[aramis.core "0.1.1"]`

#### Dispatching a group

```clj
(ns my-app.events
  (:require
    ...
    [re-frame.core :as rf]
    [aramis.core :as aramis]
    ...))
```

```cljs
(def collaborators
    [[::bar {:event-id :bar-id}]
     [::baz {:event-id :baz-id} extra-arguments]])

(rf/dispatch
    [::aramis/wait-for-all-to
     collaborators
     [::foo]]])
```

Make sure, that the collaborators events you dispatch have a hash-map containing `:event-id` as a first parameter.

#### Handling collaborators

Every event you register as a collaborator must ultimately `report` its' success.

```cljs
(rf/reg-event-fx
  ::bar
  (fn [{:keys [db]} [_ {:keys [event-id]}]]
    {:http-xhrio
     {:method :post
      :uri "/api/do-something"
      ...
      :on-success [:success event-id]}
     ...}))

(rf/reg-event-fx
  :success
  (fn [{:keys [db]} [_ event-id]]
    {:dispatch-n [[::aramis/report event-id]
                  ... others things you do on succes.
                  ]}))
```

## That's it?

To have you ready to try it out? Yes. Does that cover all of the features? Hell no!

#### Add a collaborator on the fly

Imagine that due to your actions you need to add more collaborators and hence delay the ultimate goal dispatch.
Aramis's got you covered!

- If you want to dispatch and hook to a group: 
```cljs
(rf/dispatch
  [::aramis/one-of group-id
   [::one-small-favor {:event-id :xyz}]])
```

- If you want to just hook:
```cljs
(rf/dispatch
  [::aramis/add-collaborator group-id
   [::one-small-favor {:event-id :xyz}]])
```

#### Data structure explained

Aramis stores its' data in `appdb` under `:aramis` property.

```cljs
{:group-1-id
 {:pending #{:event-1-id :event-2-id}
  :done #{:event-3-id}
  :once-done [::foo/say-hello "World"]}
 :group-2-id
 {:pending #{:event-2-id :event-99-id}
  :once-done [::foo/say-hello "Group 2"]}}
  ```
Keys of that hashmap are groups registered. 
- `:pending` contains a set of collaborators still doing their job. 
- `:once-done` - an event that will be dispatched once `:pending` set is empty.

#### One event collaborating to many groups

One event can collaborate to as many groups as you need. Finishing one group works
independently from progress in other groups.
 
#### Status of groups and collaborators


Aramis equips you with couple of subscriptions you could use to track progress of your groups:
 
```cljs
(rf/subscribe [::group-status :group1-id])
=> {:pending #{:event-1-id :event-2-id}
    :done #{:event-3-id}
    :once-done [::foo/say-hello "World"]}
```

```cljs
(rf/subscribe [::event-info :event-2-id])
=> {:status :pending
    :collaborates-to #{:group-1-id :group-2-id}}
```

```cljs
(rf/subscribe [::event-status :event-x-id])
=> :pending
```

```cljs
(subscribe [::event-status-in-group :group-1-id :event-x-id])
=> :pending
```

## Known issues and limitations

- Dispatching one event in two `::one-of` wrappers **will** disptach the event two times.
- It's assumed that status of one event as a collaborator is the same in every group it collaborates to.

## License

Copyright © 2018 Magnet Coop

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
