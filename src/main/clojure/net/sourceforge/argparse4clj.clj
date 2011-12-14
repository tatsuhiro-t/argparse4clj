(ns net.sourceforge.argparse4clj
  (:import (java.util HashMap)
           (net.sourceforge.argparse4j ArgumentParsers)
           (net.sourceforge.argparse4j.impl Arguments)
           (net.sourceforge.argparse4j.inf ArgumentParserException)
           (net.sourceforge.argparse4j.inf ArgumentType)
           (net.sourceforge.argparse4j.inf FeatureControl))
  (:require [clojure.walk])
  (:gen-class))

(defn- va [val-or-vec]
  (if (vector? val-or-vec) val-or-vec
      [val-or-vec]))

(defn- nil-to [val default]
  (if (nil? val) default val))

(defn- handle-type [type]
  (cond
   (instance? Class type) type
   (map? type) (proxy [ArgumentType]
                   []
                 (convert [parser arg value]
                   ((type :convert) parser arg value)))
   true type))

(def actions
  {:store (. Arguments store)
   :store-const (. Arguments storeConst)
   :store-true (. Arguments storeTrue)
   :store-false (. Arguments storeFalse)
   :append (. Arguments append)
   :append-const (. Arguments appendConst)
   :version (. Arguments version)
   :help (. Arguments help)})

(defn- handle-action [action]
  (cond
   (keyword? action) (actions action)
   true action))

(defn- handle-defaults [defaults]
  (let [res (new HashMap)]
    (doseq [[dest value] defaults]
      (. res put (name dest) value))
    res))

(defn arg-range [min-value max-value]
  (. Arguments range min-value max-value))

(declare setup-parser)

(defn- build-argument [parser arg-spec]
  (let [name-or-flags (into-array String (first arg-spec))
        params (fnext arg-spec)
        arg (. parser addArgument name-or-flags)]
    (doseq [[key value] params]
      (condp = key
        :action (. arg action (handle-action value))
        :choices (. arg choices value)
        :dest (. arg dest (name value))
        :const (. arg setConst value)
        :default (. arg setDefault
                    (if (= value :argparse-suppress)
                      (. FeatureControl SUPPRESS)
                      value))
        :help (. arg help value)
        :metavar (. arg metavar (into-array (va value)))
        :nargs (. arg nargs value)
        :required (. arg required value)
        :type (. arg type (handle-type value))
        nil
        ))))

(defn- build-group [parser group-spec]
  (let [params (first group-spec)
        arg-specs (fnext group-spec)
        group (. parser addArgumentGroup (params :title))]
    (doseq [[key value] params]
      (condp = key
        :description (. group description value)
        nil))
    (doseq [spec arg-specs]
      (condp = (first spec)
        :add-argument (build-argument group (next spec))))))

(defn- build-subparser [parser subparsers subparser-spec]
  (let [command (first subparser-spec)
        params (fnext subparser-spec)
        parser-specs (fnext (next subparser-spec))
        subparser (. subparsers addParser command
                     (nil-to (params :add-help) true)
                     (if (nil? (params :prefix-chars))
                       (. parser getPrefixChars) (params :prefix-chars)))]
    (doseq [[key value] params]
      (condp = key
        :help (. subparser help value)
        nil))
    (setup-parser subparser params parser-specs)))

(defn- build-subparsers [parser subparsers-spec]
  (let [subparsers (. parser addSubparsers)
        params (first subparsers-spec)
        subparser-specs (fnext subparsers-spec)]
    (doseq [[key value] params]
      (condp = key
        :description (. subparsers description value)
        :dest (. subparsers dest value)
        :help (. subparsers help value)
        :metavar (. subparsers metavar value)
        :title (. subparsers title value)
        nil))
    (doseq [spec subparser-specs]
      (condp = (first spec)
        :add-parser
        (build-subparser parser subparsers (next spec))))))

(defn- setup-parser [parser params parser-specs]
  (doseq [[key value] params]
    (condp = key
      :default-help (. parser defaultHelp value)
      :description (. parser description value)
      :epilog (. parser epilog value)
      :defaults (. parser setDefaults (handle-defaults value))
      :version (. parser version value)
      nil))
  (doseq [spec parser-specs]
    (let [method (first spec)]
      (condp = method
        :add-argument (build-argument parser (next spec))
        :add-argument-group (build-group parser (next spec))
        :add-subparsers (build-subparsers parser (next spec))
        ))))

(defn- build-parser [params specs]
  (let [parser (. ArgumentParsers newArgumentParser
              (params :prog)
              (nil-to (params :add-help) true)
              (nil-to (params :prefix-chars) "-"))]
    (setup-parser parser params specs)
    parser))

(defn new-argument-parser [params & specs]
  (build-parser params specs))

(defn parse-args
  [args parser]
  (let [result (new HashMap)]
    (try
      (. parser parseArgs (into-array String args) result)
      (clojure.walk/keywordize-keys (into {} result))
      (catch ArgumentParserException e
        (. parser handleError e)
        (. System exit 1))
      (catch RuntimeException e
        ;; If ArgumentParserException is thrown in delegated method,
        ;; it is wrapped with RuntimeException.
        (let [cause (. e getCause)]
          (if (instance? ArgumentParserException cause)
            (do
              (. parser handleError cause)
              (. System exit 1))
            (throw e)))))))
            
(defn add-argument [name-or-flags & [params]]
  [:add-argument (va name-or-flags) params])

(defn add-argument-group [params & arg-specs]
  [:add-argument-group params arg-specs])

(defn add-parser [command & [params & parser-specs]]
  [:add-parser command params parser-specs])

(defn add-subparsers [params & subparser-specs]
  [:add-subparsers params subparser-specs])
