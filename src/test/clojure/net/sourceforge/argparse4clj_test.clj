(ns net.sourceforge.argparse4clj-test
  (:import [net.sourceforge.argparse4j.inf ArgumentParser])
  (:use clojure.test net.sourceforge.argparse4clj))

(deftest argument-action
  (def parser (new-argument-parser
               {:prog "prog"}
               (add-argument ["-f" "--foo"] {:dest :dest, :default "def"})
               (add-argument "-a" {:const :a, :action :store-const})
               (add-argument "-b" {:const :b, :action :append-const})
               (add-argument "-c" {:action :append})
               (add-argument "-d" {:action :store-true})
               (add-argument "-e" {:action :store-false})
               ))
  (def args (parse-args ["-a" "-b" "-b" "-d" "-e"] parser))
  (is (= "def" (args :dest)))
  (is (= :a (args :a)))
  (is (= [:b :b] (args :b)))
  (is (= true (args :d)))
  (is (= false (args :e)))
  )

(deftest argument-choices
  (def parser (new-argument-parser
               {:prog "prog"}
               (add-argument "-a" {:choices ["foo" "bar"]})
               (add-argument "-b" {:choices (range 0 10), :type Integer})
               (add-argument "-c" {:choices [1 2 3], :type Integer})
               (add-argument "-d" {:choices (between 1 65535),
                                   :type Integer})))
  (def args (parse-args ["-a" "foo" "-b" "9" "-c" "2"] parser))
  (is (= "foo" (args :a)))
  (is (= 9 (args :b)))
  (is (= 2 (args :c)))
  )

(deftest argument-nargs
  (def parser (new-argument-parser
               {:prog "prog"}
               (add-argument "-a" {:nargs 2})
               (add-argument "-b" {:nargs "*"})
               (add-argument "-c" {:nargs "+"})
               (add-argument "-d" {:nargs "?"})))
  (def args (parse-args ["-a" "foo" "bar" "-b" "baz"] parser))
  (is (= ["foo" "bar"] (args :a)))
  (is (= ["baz"] (args :b)))
  )

(deftest argument-metavar
  (def parser (new-argument-parser
               {:prog "prog"}
               (add-argument "-a" {:metavar "M"})
               (add-argument "-b" {:nargs 2, :metavar ["N" "M"]})))
  (is (.contains (. parser formatHelp) "[-a M] [-b N M]"))
  )

(deftest argument-type
  (def parser (new-argument-parser
               {:prog "prog"}
               (add-argument "-a" {:type #(Integer. %)})))
  (def args (parse-args ["-a" "1000007"] parser))
  (is (= 1000007 (args :a)))
  )

(deftest argument-action-append
  (def parser (new-argument-parser
               {:prog "prog"}
               (add-argument "-a" {:default [], :action :append})
               (add-argument "-b" {:default [], :action :append-const,
                                   :const :cons})))
  (def args (parse-args ["-a" "1" "-a" "2" "-b" "-b"] parser))
  (is (= ["1" "2"] (args :a)))
  (is (= [:cons :cons] (args :b)))
  )

(deftest parser-defaults
  (def parser (new-argument-parser
               {:prog "prog", :defaults {:foo "all", :bar "baz"}}))
  (is (= "all" (. parser getDefault "foo")))
  )

(deftest default-suppress
  (def parser (new-argument-parser
               {:prog "prog"}
               (add-argument "-a" {:default :argparse.suppress})))
  (def args (parse-args [] parser))
  (is (not (contains? args :a)))
  )

(deftest subparser
  (def parser
    (new-argument-parser
     {:prog "prog"}
     (add-subparsers
      {:description "description for sub-commands",
       :help "help for sub-commands",
       :metavar "COMMANDS",
       :dest "command",
       :title "title for sub-commands"}
      (add-parser
       "install"
       {:default-help true,
        :description "description for install",
        :epilog "epilog for install",
        :help "help for install",
        :version "version"}
       (add-argument "-f")))))
  (def args (parse-args ["install" "-f" "foo"] parser))
  (is (= "install" (args :command)))
  (is (= "foo" (args :f)))
  )

(deftest group
  (def parser
    (new-argument-parser
     {:prog "prog"}
     (add-argument-group {:title "title for group",
                          :description "desc for group"}
                         (add-argument "-f"))))
  (def help (. parser formatHelp))
  (is (.contains help "title for group"))
  (is (.contains help "desc for group"))
  )
