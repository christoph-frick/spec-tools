(ns spec-tools.perf-test
  (:require [clojure.spec :as spec]
            [schema.core :as schema]
            [schema.coerce :as coerce]
            [spec-tools.core :as st]
            [spec-tools.specs :as sts]
            [criterium.core :as cc]
            [clojure.spec :as s]
            [spec-tools.conform :as stc]))

;;
;; start repl with `lein perf repl`
;; perf measured with the following setup:
;;
;; Model Name:            MacBook Pro
;; Model Identifier:      MacBookPro11,3
;; Processor Name:        Intel Core i7
;; Processor Speed:       2,5 GHz
;; Number of Processors:  1
;; Total Number of Cores: 4
;; L2 Cache (per Core):   256 KB
;; L3 Cache:              6 MB
;; Memory:                16 GB
;;

(spec/def ::age (spec/and integer? #(> % 10)))
(spec/def ::x-age (spec/and sts/integer? #(> % 10)))

(def age (schema/constrained schema/Int #(> % 10)))

(defn raw-title [color s]
  (println (str color (apply str (repeat (count s) "#")) "\u001B[0m"))
  (println (str color s "\u001B[0m"))
  (println (str color (apply str (repeat (count s) "#")) "\u001B[0m")))

(def title (partial raw-title "\u001B[35m"))
(def suite (partial raw-title "\u001B[32m"))

(schema/check age 12)

(defn valid-test []

  (suite "valid?")

  ; 1260ns
  ; 81ns (alpha12)
  ; 84ns (alpha14)
  (title "spec: integer?")
  (let [call #(spec/valid? ::age 12)]
    (assert (call))
    (cc/quick-bench
      (call)))

  ; 1480ns
  ; 77ns (alpha12)
  ; 82ns (alpha14)
  (title "spec: x-integer?")
  (let [call #(spec/valid? ::x-age 12)]
    (assert (call))
    (cc/quick-bench
      (call)))

  ; 430ns
  (title "schema: s/Int")
  (let [call #(schema/check age 12)]
    (assert (nil? (call)))
    (cc/quick-bench
      (call)))

  ; 31ns
  (title "schema: s/Int (compiled)")
  (let [checker (schema/checker age)
        call #(checker 12)]
    (assert (nil? (call)))
    (cc/quick-bench
      (call))))

(defn conform-test []

  (suite "no-op conform")

  ; 1315ns
  ;  100ns (alpha12)
  ;   81ns (alpha14)
  (title "spec: integer?")
  (let [call #(spec/conform ::age 12)]
    (assert (= (call) 12))
    (cc/quick-bench
      (call)))

  ; 1430ns
  ;   95ns (alpha12)
  ;   83ns (alpha14)
  (title "spec: x-integer?")
  (let [call #(st/conform ::x-age 12)]
    (assert (= (call) 12))
    (cc/quick-bench
      (call)))

  ; 425ns
  (title "schema: s/Int")
  (let [call #((coerce/coercer age (constantly nil)) 12)]
    (assert (= (call) 12))
    (cc/quick-bench
      (call)))

  ; 25ns
  (title "schema: s/Int (compiled)")
  (let [coercer (coerce/coercer age (constantly nil))
        call #(coercer 12)]
    (assert (= (call) 12))
    (cc/quick-bench
      (call))))

(defn conform-test2 []

  (suite "conforming set of keywords")

  (let [sizes-spec (spec/coll-of (spec/and sts/keyword? #{:L :M :S}) :into #{})
        sizes-schema #{(schema/enum :L :M :S)}]

    ; 4300ns
    ; 1440ns (alpha12)
    ; 1160ns (alpha14)
    (title "spec: conform keyword enum")
    (let [call #(st/conform sizes-spec ["L" "M"] stc/string-conformers)]
      (assert (= (call) #{:L :M}))
      (cc/quick-bench
        (call)))

    ; 3700ns
    ; 990ns (alpha12)
    ; 990ns (alpha14)
    (title "spec: conform keyword enum - no-op")
    (let [call #(st/conform sizes-spec #{:L :M} stc/string-conformers)]
      (assert (= (call) #{:L :M}))
      (cc/quick-bench
        (call)))

    ; 1100ns
    (title "schema: conform keyword enum")
    (let [coercer (coerce/coercer sizes-schema coerce/string-coercion-matcher)
          call #(coercer ["L" "M"])]
      (assert (= (call) #{:L :M}))
      (cc/quick-bench
        (call)))

    ; 780ns
    (title "schema: conform keyword enum - no-op")
    (let [coercer (coerce/coercer sizes-schema coerce/string-coercion-matcher)
          call #(coercer #{:L :M})]
      (assert (= (call) #{:L :M}))
      (cc/quick-bench
        (call)))))

(s/def ::order-id sts/integer?)
(s/def ::product-id sts/integer?)
(s/def ::product-name sts/string?)
(s/def ::price sts/double?)
(s/def ::quantity sts/integer?)
(s/def ::name sts/string?)
(s/def ::zip sts/integer?)
(s/def ::street string?)
(s/def ::country (s/and sts/keyword? #{:fi :po}))
(s/def ::receiver (s/keys :req-un [::name ::street ::zip]
                          :opt-un [::country]))
(s/def ::orderline (s/keys :req-un [::product-id ::price]
                           :req.un [::product-name]))
(s/def ::orderlines (s/coll-of ::orderline))
(s/def ::order (s/keys :req-un [::order-id ::orderlines ::receiver]))
(s/def ::order-with-line (s/and ::order #(> (::orderlines 1))))

(s/form
  (s/cat ::first keyword?
         :integer-lists (s/+
                          (s/coll-of
                            (s/keys :req-un [::order-id
                                             ::orderlines
                                             ::receiver])))))

(schema/defschema Order
  {:order-id Long
   :orderlines [{:product-id Long
                 :price Double
                 (schema/optional-key :product-name) String}]
   :receiver {:name String
              :street String
              :zip Long
              (schema/optional-key :country) (schema/enum :fi :po)}})

(def sample-order-valid
  {:order-id 12
   :orderlines [{:product-id 1
                 :price 12.3}
                {:product-id 2
                 :price 9.99
                 :product-name "token"}]
   :receiver {:name "Tommi"
              :street "Kotikatu 2"
              :zip 33310
              :country :fi}})

(def sample-order
  {:order-id "12"
   :orderlines [{:product-id "1"
                 :price "12.3"}
                {:product-id "2"
                 :price "9.99"
                 :product-name "token"}]
   :receiver {:name "Tommi"
              :street "Kotikatu 2"
              :zip "33310"
              :country "fi"}})

(defn conform-test3 []

  (suite "conforming a nested map")

  ; 4.5µs (alpha12)
  ; 3.9µs (alpha14)
  (title "spec: conform")
  (let [call #(st/conform ::order sample-order stc/string-conformers)]
    (assert (= (call) sample-order-valid))
    (cc/quick-bench
      (call)))

  ; 2.8µs (alpha12)
  ; 2.7µs (alpha14)
  (title "spec: conform - no-op")
  (let [call #(st/conform ::order sample-order-valid stc/string-conformers)]
    (assert (= (call) sample-order-valid))
    (cc/quick-bench
      (call)))

  ; 9.1µs
  (title "schema: conform")
  (let [coercer (coerce/coercer Order coerce/string-coercion-matcher)
        call #(coercer sample-order)]
    (assert (= (call) sample-order-valid))
    (cc/quick-bench
      (call)))

  ; 9.3µs
  (title "schema: conform - no-op")
  (let [coercer (coerce/coercer Order coerce/string-coercion-matcher)
        call #(coercer sample-order-valid)]
    (assert (= (call) sample-order-valid))
    (cc/quick-bench
      (call))))

(comment
  (valid-test)
  (conform-test)
  (conform-test2)
  (conform-test3))
