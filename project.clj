(defproject metosin/lomakkeet "0.2.0-SNAPSHOT"
  :description "Proof of concept: Form library for [om|re-frame]"
  :url "https://github.com/metosin/lomakkeet"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src"]
  :dependencies [[org.clojure/clojure "1.6.0" :scope "provided"]
                 [org.clojure/clojurescript "0.0-3126" :scope "provided"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [prismatic/schema "0.4.0"]
                 [reagent "0.5.0-alpha3"]
                 [re-frame "0.2.0"]
                 [metosin/schema-tools "0.3.0"]
                 [com.andrewmcveigh/cljs-time "0.3.2"]
                 [cljsjs/pikaday "1.2.0-3"]]
  :plugins [[lein-pprint "1.1.2"]]

  :cljsbuild
  {:builds {:test {:source-paths ["src" "test"]
                   :notify-command ["phantomjs" :cljs.test/runner
                                    "test/phantomjs-shims.js"
                                    "target/testable.js"]
                   :compiler {:output-to  "target/testable.js"
                              :output-dir "target/cljsbuild-test"
                              :source-map "target/cljsbuild-test.js.map"
                              :optimizations :whitespace
                              :pretty-print  true}}}
   :test-commands {"test" ["phantomjs" :runner
                           "test/phantomjs-shims.js"
                           "target/testable.js"]}}

  :profiles
  {:dev
   {:source-paths ["dev-src/clj"]
    :plugins [[lein-cljsbuild "1.0.4"]
              [com.cemerick/clojurescript.test "0.3.3"]]}})
