(defproject clj-cayley-dickson "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0-alpha7"]
                 [org.apache.commons/commons-math3 "3.6.1"]]
  :main ^:skip-aot og.cayley-dickson.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
