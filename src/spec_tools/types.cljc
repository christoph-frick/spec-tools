(ns spec-tools.types
  (:require [spec-tools.impl :as impl]
            [clojure.string :as str]))

(defn- dispatch [x]
  (cond

    ;; symbol
    (symbol? x)
    (impl/clojure-core-symbol-or-any x)

    ;; a from
    (seq? x)
    (impl/clojure-core-symbol-or-any (first x))

    ;; default
    :else x))

(defn- error-message [x]
  (str
    "Can't resolve type for dispatch value `" (dispatch x) "`. "
    "Provide a `:hint` for the spec or add a dispatch "
    "function for `spec-tools.types/resolve-type`. Spec: "
    (str/replace (str x) #"\n" " ") "\n"))

(defmulti resolve-type #'dispatch :default ::default)

(defmethod resolve-type ::default [x]
  (println (error-message x)))

(defn resolve-type-or-fail [x]
  (if (contains? (methods resolve-type) x)
    (resolve-type x)
    (throw (ex-info (error-message x) {:spec x}))))

(defn- all-types []
  {:long
   :double
   :boolean
   :string
   :keyword
   :symbol
   :uuid
   :uri
   :bigdec
   :date
   :ratio
   :map
   :set
   :vector})

(defmethod resolve-type 'clojure.core/any? [_] nil)
(defmethod resolve-type 'clojure.core/some? [_] nil)
(defmethod resolve-type 'clojure.core/number? [_] :double)
(defmethod resolve-type 'clojure.core/integer? [_] :long)
(defmethod resolve-type 'clojure.core/int? [_] :long)
(defmethod resolve-type 'clojure.core/pos-int? [_] :long)
(defmethod resolve-type 'clojure.core/neg-int? [_] :long)
(defmethod resolve-type 'clojure.core/nat-int? [_] :long)
(defmethod resolve-type 'clojure.core/float? [_] :double)
(defmethod resolve-type 'clojure.core/double? [_] :double)
(defmethod resolve-type 'clojure.core/boolean? [_] :boolean)
(defmethod resolve-type 'clojure.core/string? [_] :string)
(defmethod resolve-type 'clojure.core/ident? [_] :keyword)
(defmethod resolve-type 'clojure.core/simple-ident? [_] :keyword)
(defmethod resolve-type 'clojure.core/qualified-ident? [_] :keyword)
(defmethod resolve-type 'clojure.core/keyword? [_] :keyword)
(defmethod resolve-type 'clojure.core/simple-keyword? [_] :keyword)
(defmethod resolve-type 'clojure.core/qualified-keyword? [_] :keyword)
(defmethod resolve-type 'clojure.core/symbol? [_] :symbol)
(defmethod resolve-type 'clojure.core/simple-symbol? [_] :symbol)
(defmethod resolve-type 'clojure.core/qualified-symbol? [_] :symbol)
(defmethod resolve-type 'clojure.core/uuid? [_] :uuid)
(defmethod resolve-type 'clojure.core/uri? [_] :uri)
(defmethod resolve-type 'clojure.core/bigdec? [_] :bigdec)
(defmethod resolve-type 'clojure.core/inst? [_] :date)
(defmethod resolve-type 'clojure.core/seqable? [_] nil)
(defmethod resolve-type 'clojure.core/indexed? [_] nil)
(defmethod resolve-type 'clojure.core/map? [_] nil)
(defmethod resolve-type 'clojure.core/vector? [_] nil)
(defmethod resolve-type 'clojure.core/list? [_] nil)
(defmethod resolve-type 'clojure.core/seq? [_] nil)
(defmethod resolve-type 'clojure.core/char? [_] nil)
(defmethod resolve-type 'clojure.core/set? [_] nil)
(defmethod resolve-type 'clojure.core/nil? [_] :nil)
(defmethod resolve-type 'clojure.core/false? [_] :boolean)
(defmethod resolve-type 'clojure.core/true? [_] :boolean)
(defmethod resolve-type 'clojure.core/zero? [_] :long)
(defmethod resolve-type 'clojure.core/rational? [_] :long)
(defmethod resolve-type 'clojure.core/coll? [_] nil)
(defmethod resolve-type 'clojure.core/empty? [_] nil)
(defmethod resolve-type 'clojure.core/associative? [_] nil)
(defmethod resolve-type 'clojure.core/sequential? [_] nil)
(defmethod resolve-type 'clojure.core/ratio? [_] :ratio)
(defmethod resolve-type 'clojure.core/bytes? [_] nil)

; keys
(defmethod resolve-type 'clojure.spec/keys [_] :map)

; or
(defmethod resolve-type 'clojure.spec/or [x] nil)

; and
(defmethod resolve-type 'clojure.spec/and [x] nil)

; merge

; every
(defmethod resolve-type 'clojure.spec/every [x]
  (let [{:keys [into]} (apply hash-map (drop 2 x))]
    (cond
      (map? into) :map
      (set? into) :set
      :else :vector)))

; every-ks

; coll-of
(defmethod resolve-type 'clojure.spec/coll-of [x]
  (let [{:keys [into]} (apply hash-map (drop 2 x))]
    (cond
      (map? into) :map
      (set? into) :set
      :else :vector)))

; map-of
(defmethod resolve-type 'clojure.spec/map-of [_] :map)

; *
; +
; ?
; alt
; cat
; &
; tuple
; keys*
; nilable
