(ns sample-tx-fns.fix-user-zip
  (:require [datomic.client.api :as d]))


(def deprecated-values-q
  '[:find ?e ?v
    :in $ ?depr-attr
    :where
    [?e ?depr-attr ?v]])

(defn port-attr-data
  [conn depr-attr new-attr xform]
  (let [old-data (d/q deprecated-values-q
                      (d/db conn)
                      depr-attr)]
    (for [[e old-v] old-data
          :let [new-data (xform old-v)]]
      {:db/id e
       new-attr new-data})))

(defn deprecate-attr
  ([depr-attr]
   [{:db/id [:db/ident depr-attr]
     :schema/deprecated true}])
  ([depr-attr new-attr]
   [{:db/id [:db/ident depr-attr]
     :schema/deprecated true
     :schema/see-instead new-attr}]))

(defn migrate
  "The `:user/zip` schema attribute was added to the schema as a
  `:db.type/long`, which turns out to be insufficient for several
  reasons.

  Replace `:user/zip` with a new attribute, `:user/zipcode`...

  - Mark the old attribute deprecated
  - Port any old attribute data to the new attribute"
  [conn]
  (concat (deprecate-attr :user/zip :user/zip-code)
          (port-attr-data conn :user/zip :user/zip-code str)))


