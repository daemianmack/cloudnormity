(ns cloudnormity.protocols
  "Abstraction layer letting us write tests without needing a live
  Datomic connection or a more formal means of examining state
  transitions in a persisted store.

  Expedient, but shitty. Replace with Datomic 'local dev' testing
  strategy when that emerges."
  (:require [datomic.client.api :as d]
            [datomic.client.impl.shared :as datomic-impl]))


(defprotocol FakeableConn
  (db [this])
  (transact [this tx-data]))

(defprotocol FakeableDb
  (has-norm? [this tracking-attr norm-map])
  (has-attr? [this attr-name]))


(extend-type datomic.client.impl.shared.Connection
  FakeableConn
  (db [this]
    (d/db this))
  (transact [this tx-data]
    (d/transact this {:tx-data tx-data})))

(extend-type datomic.client.impl.shared.Db
  FakeableDb
  (has-attr? [this attr-name]
    (-> (d/pull this '[:db/id] [:db/ident attr-name])
        :db/id
        boolean))
  (has-norm? [this tracking-attr norm-map]
    (boolean
     (seq (d/q
           '[:find ?e
             :in $ ?tracking-attr ?norm-name
             :where [?e ?tracking-attr ?norm-name]]
           this
           tracking-attr
           (:name norm-map))))))

(extend-type clojure.lang.Atom

  FakeableConn
  (db [this]
    this)
  (transact [this tx-data]
    (swap! this into tx-data))

  FakeableDb
  ;; This representation lacks notion of EIDs; tests are solely
  ;; concerned with presence of attr and attr/val pairs.
  (has-attr? [this attr-name]
    (-> (keep attr-name @this)
        seq
        boolean))
  (has-norm? [this tracking-attr norm-map]
    (-> #{{tracking-attr (:name norm-map)}}
        (some @this)
        boolean)))
