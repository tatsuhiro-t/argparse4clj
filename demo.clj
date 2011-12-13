(ns demo (:use [net.sourceforge.argparse4clj]))

(def args
 (parse-args
  *command-line-args*
  {:prog "prog", :description "Process some integers."}
  (add-argument "integers" {:metavar "N"
                            :type Long
                            :nargs "+"
                            :help "an integer for the accumulator"})
  (add-argument "--sum" {:dest "accumulate"
                         :action :store-const
                         :const +
                         :default max
                         :help "sum the integers (default: find the max)"})))
(println (reduce (args :accumulate) (args :integers)))
