Argparse4clj - The command line argument parser library for Clojure

The argparse4clj is a command line argument parser library for
Clojure. Argparse4clj is actually a wrapper library for argparse4j.

Sample code:
-------------------------------------------------------------------------------
(ns demo (:use [net.sourceforge.argparse4clj]))

(def args
 (parse-args
  *command-line-args*
  (new-argument-parser
   {:prog "prog", :description "Process some integers."}
   (add-argument "integers" {:metavar "N", :type Integer, :nargs "+"
                             :help "an integer for the accumulator"})
   (add-argument "--sum" {:dest :accumulate, :action :store-const,
                          :const +, :default max
                          :help "sum the integers (default: find the max)"}))))
(println (apply (args :accumulate) (args :integers)))
-------------------------------------------------------------------------------

With -h option, the above script prints:

-------------------------------------------------------------------------------
usage: prog [-h] [--sum] N [N ...]

Process some integers.

positional arguments:
  N                      an integer for the accumulator

optional arguments:
  -h, --help             show this help message and exit
  --sum                  sum the integers (default: find the max)
-------------------------------------------------------------------------------

Project has just started and no documentation at the moment.
  
Here is summary of features:
  
  * Supported positional arguments and optional arguments.

  * Variable number of arguments.

  * Generates well formatted line-wrapped help message.

  * Suggests optional arguments/sub-command if unrecognized
    arguments/sub-command were given, e.g. "unrecognized argument
    '--tpye'. Did you mean: --type".

  * Takes into account East Asian Width ambiguous characters when
    line-wrap.

  * Sub-commands like, git add.

  * Customizable option prefix characters, e.g. '+f' and '/h'.

  * Print default values in help message.

  * Choice from given collection of values.

  * Type conversion from option strings.

  * Can directly assign values into user defined classes using
    annotation.

  * Group arguments so that it will be printed in help message in more
    readable way.

  * Read additional arguments from file.
