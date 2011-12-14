(ns demo (:use [net.sourceforge.argparse4clj]))

(def args
 (parse-args
  *command-line-args*
  (new-argument-parser
   {:prog "prog", :description "Process some integers."}
   (add-argument "integers" {:metavar "N", :type Integer, :nargs "+"
                             :help "an integer for the accumulator"})
   (add-argument "--sum" {:dest "accumulate", :action :store-const,
                          :const +, :default max
                          :help "sum the integers (default: find the max)"}))))
(println (apply (args :accumulate) (args :integers)))
