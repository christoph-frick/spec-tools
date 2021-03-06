(ns spec-tools.conform
  #?(:cljs (:refer-clojure :exclude [Inst Keyword UUID]))
  (:require [clojure.spec :as s]
    #?@(:cljs [[goog.date.UtcDateTime]
               [goog.date.Date]])
            [clojure.string :as str])
  #?(:clj
     (:import (java.util Date UUID)
              (java.time Instant))))

;;
;; Strings
;;

(defn string->long [_ x]
  (if (string? x)
    (try
      #?(:clj  (Long/parseLong x)
         :cljs (js/parseInt x 10))
      (catch #?(:clj  Exception
                :cljs js/Error) _
        ::s/invalid))))

(defn string->double [_ x]
  (if (string? x)
    (try
      #?(:clj  (Double/parseDouble x)
         :cljs (js/parseFloat x))
      (catch #?(:clj  Exception
                :cljs js/Error) _
        ::s/invalid))))

(defn string->keyword [_ x]
  (if (string? x)
    (keyword x)))

(defn string->boolean [_ x]
  (if (string? x)
    (cond
      (= "true" x) true
      (= "false" x) false
      :else ::s/invalid)))

(defn string->uuid [_ x]
  (if (string? x)
    (try
      #?(:clj  (UUID/fromString x)
         :cljs (uuid x))
      (catch #?(:clj  Exception
                :cljs js/Error) _
        ::s/invalid))))

(defn string->date [_ x]
  (if (string? x)
    (try
      #?(:clj  (Date/from
                 (Instant/parse x))
         :cljs (js/Date. (.getTime (goog.date.UtcDateTime.fromIsoString x))))
      (catch #?(:clj  Exception
                :cljs js/Error) _
        ::s/invalid))))

(defn string->symbol [_ x]
  (if (string? x)
    (symbol x)))

(defn string->nil [_ x]
  (if-not (str/blank? x)
    ::s/invalid))

;;
;; Maps
;;

(defn strip-extra-keys [{:keys [:keys pred]} x]
  (if (map? x)
    (s/conform pred (select-keys x keys))
    x))

;;
;; conformers
;;

(def json-conformers
  {:keyword string->keyword
   :uuid string->uuid
   :date string->date
   :symbol string->symbol
   ;; TODO: implement
   :uri nil
   :bigdec nil
   :ratio nil})

(def string-conformers
  (merge
    json-conformers
    {:long string->long
     :double string->double
     :boolean string->boolean
     :nil string->nil
     :string nil}))
