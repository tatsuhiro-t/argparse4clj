(ns net.sourceforge.argparse4clj
  (:import (java.util HashMap)
           (net.sourceforge.argparse4j ArgumentParsers)
           (net.sourceforge.argparse4j.impl Arguments)
           (net.sourceforge.argparse4j.inf ArgumentParserException)
           (net.sourceforge.argparse4j.inf ArgumentType)
           (net.sourceforge.argparse4j.inf ArgumentChoice)
           (net.sourceforge.argparse4j.inf ArgumentAction)
           (net.sourceforge.argparse4j.inf FeatureControl))
  (:require [clojure.walk])
  (:gen-class))

(declare setup-parser)

(defn- va [val-or-vec]
  (if (vector? val-or-vec) val-or-vec
      [val-or-vec]))

(defn- non-nil [val default]
  (if (nil? val) default val))

(def append-action
  (proxy [ArgumentAction] []
    (consumeArgument [] true)
    (onAttach [arg])
    (run [parser arg attrs flag value]
      (let [dest (.getDest arg)
            obj (.get attrs dest)]
        (if (vector? obj)
          (.put attrs dest (conj obj value))
          (.put attrs dest [value]))))))

(def append-const-action
  (proxy [ArgumentAction] []
    (consumeArgument [] false)
    (onAttach [arg])
    (run [parser arg attrs flag value]
      (let [dest (.getDest arg)
            const (.getConst arg)
            obj (.get attrs dest)]
        (if (vector? obj)
          (.put attrs dest (conj obj const))
          (.put attrs dest [const]))))))

(def actions
  {:store (. Arguments store)
   :store-const (. Arguments storeConst)
   :store-true (. Arguments storeTrue)
   :store-false (. Arguments storeFalse)
   :append append-action
   :append-const append-const-action
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

(defn- handle-choices [choices]
  (cond
   (coll? choices) (proxy [ArgumentChoice] []
                     (contains [val] (.contains choices val))
                     (textualFormat [] (str (vec choices))))
   (instance? ArgumentChoice choices) choices
   true choices))

(defn- handle-type [type]
  (cond
   (instance? Class type) type
   (fn? type) (proxy [ArgumentType] []
                (convert [parser arg value]
                  (type value)))
   (map? type) (proxy [ArgumentType]
                   []
                 (convert [parser arg value]
                   ((type :convert) parser arg value)))
   true type))

(defn- build-argument [parser arg-spec]
  (let [name-or-flags (into-array String (first arg-spec))
        params (fnext arg-spec)
        arg (. parser addArgument name-or-flags)]
    (doseq [[key value] params]
      (condp = key
        :action (. arg action (handle-action value))
        :choices (. arg choices (handle-choices value))
        :dest (. arg dest (name value))
        :const (. arg setConst value)
        :default (. arg setDefault
                    (if (= value :argparse.suppress)
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
                     (non-nil (params :add-help) true)
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
        :dest (. subparsers dest (name value))
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
                  (non-nil (params :add-help) true)
                  (non-nil (params :prefix-chars) "-")
                  (params :from-file-prefix-chars))]
    (setup-parser parser params specs)
    parser))

(defn new-argument-parser [params & specs]
  (build-parser params specs))

(defn parse-args
  ([parser] (parse-args *command-line-args* parser) )
  ([args parser]
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
               (throw e))))))))

(defn add-argument
  ([name-or-flags] (add-argument name-or-flags {}))
  ([name-or-flags params]
     [:add-argument (va name-or-flags) params]))

(defn add-argument-group [params & arg-specs]
  [:add-argument-group params arg-specs])

(defn add-parser
  ([command] (add-parser command {}))
  ([command params & parser-specs]
     [:add-parser command params parser-specs]))

(defn add-subparsers [params & subparser-specs]
  [:add-subparsers params subparser-specs])

(defn xrange
  "Used as value of :type in add-argument function to specify
  acceptable range of option value. This does not cripple help
  message if range is large. The range is from min-value to
  max-value, including min-value but excluding max-value."
  [min-value max-value]
  (. Arguments range min-value (- max-value 1)))
