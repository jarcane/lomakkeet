(ns lomakkeet.autocomplete
  (:require-macros lomakkeet.autocomplete
                   [cljs.core.async.macros :refer [go]])
  (:require [clojure.string :as string]
            [cljs.core.async :refer [put! chan close!]]
            [reagent.core :as reagent :refer [atom]]
            [reagent.ratom :refer-macros [run! reaction]]
            [re-frame.core :refer [dispatch]]
            goog.events
            [lomakkeet.core :as f]
            [lomakkeet.util :as util]
            [lomakkeet.impl.mixins :as mixins]))

(defn normalize [s]
  ; FIXME:
  (some-> s string/lower-case #_remove-accents))

(defn highlight-string [s query]
  ; FIXME: If normalized strings length is different, this will break!
  (if query
    (loop [rst s
           r [:span]]
      ; Find first matching term
      (let [[i match]
            (reduce (fn [[i :as acc] term]
                      (let [x (.indexOf (normalize rst) term)]
                        (if (and (not= x -1) (or (= i -1) (< x i)))
                          [x (subs rst x (+ x (count term)))]
                          acc)))
                    [-1 nil]
                    query)
            pre (subs rst 0 i)]
        (if (not= i -1)
          (recur (subs rst (+ i (count match)))
                 (if (seq pre)
                   (conj r pre [:span.highlight match])
                   (conj r [:span.highlight match])))
          (if (seq rst)
            (conj r rst)
            r))))
    [:span s]))

(defn- query-match? [term-match? v query]
  (every? (partial term-match? v) query))

(defn matches [term-match? v query]
  (let [m (group-by (partial term-match? v) query)]
    [(get m true) (get m false)]))

(defn ->query [search]
  (some-> search
    (.toLowerCase)
    (.split #" ")
    (->> (remove empty?))))

(defn filter-results [term-match? n items query]
  (reset! n -1)
  (->> (if query
         (filter #(query-match? term-match? % query) items)
         items)
       (map (fn [v]
              (assoc v :i (swap! n inc))))))

(defn blur [open? search e]
  (when (.-relatedTarget e)
    (reset! open? false)
    (reset! search nil))
  true)

(defn click [open? e]
  (reset! open? true)
  (.stopPropagation e)
  true)

(defn focus [open? input _]
  (if-not @open?
    (reset! input ""))
  (reset! open? true)
  true)

(defn change [search e]
  (reset! search (.. e -target -value))
  true)

(defn key-down [open? search results selected n find-by-selection cb e]
  (letfn [(change-selection  [f e]
            (swap! selected (comp (partial util/limit 0 @n) f))
            (.preventDefault e)
            (.stopPropagation e))]
    (reset! open? true)

    (case (.-key e)
      "Enter" (when-let [v (find-by-selection @results @selected)]
                (cb v)
                (reset! open? false)
                (reset! search nil))
      "ArrowUp" (change-selection dec e)
      "ArrowDown" (change-selection inc e)
      nil)))

(defn find-by-selection [data x]
  (some (fn [{:keys [i] :as v}]
          (if (= i x) v))
        data))

(defn render-item
  [parent item query selected cb {:keys [item->key item->text]}]
  (reagent/create-class
    {:reagent-render
     (fn []
       [:div
        {:on-click #(cb item)
         :class (if (= (:i item) @selected) "active")
         :data-selectable true}
        (highlight-string (item->text item) @query)])}))

(defn renderer
  [coll query selected cb {:keys [item->key] :as opts}]
  (let []
    (reagent/create-class
      {:render
       (fn [this]
         [:div.selectize-dropdown-content
          ; FIXME: keep-visible!
          (for [item @coll]
            ^{:key (item->key item)}
            [render-item item query selected cb opts])])})))

(defn autocomplete*
  [form {:keys [ks value->text item->key loading-el load-items term-match?]
         :or {value->text identity
              loading-el [:div "Loading..."]}
         :as opts}]
  (let [open? (atom false)
        closable (mixins/create-closable open?)
        search (atom nil)
        items (atom nil)
        value (reaction (get-in (:lomakkeet.core/value @form) ks))

        search-chan (chan)
        delayed-search (util/debounce search-chan 100)
        query (atom nil)
        selected (atom 0)

        n (atom -1)
        results (reaction (filter-results term-match? n @items @query))

        cb
        (fn [v]
          (dispatch [:update-value {:ks ks :value (item->key v)}])
          (reset! open? false))]
    (swap! items load-items)
    (go (loop [] (let [x (<! delayed-search)]
                   (when x
                     (reset! query (->query x))
                     (recur)))))
    (run! (if-let [s @search] (put! search-chan s)))
    (reagent/create-class
      {:did-unmount
       (fn [_]
         (close! search-chan)
         (closable))
       :reagent-render
       (fn []
         [:div.selectize-control.single
          [:input.selectize-input
           {:on-focus  (partial focus open? search)
            :on-blur   (partial blur open? search)
            :on-click  (partial click open?)
            :on-change (partial change search)
            ; Needs a horrible amount of parameters, perhaps using wrap could help?
            ; write selected -> check limits etc.
            :on-key-down (partial key-down open? search results selected n find-by-selection cb)
            :value (or (if @open?
                         @search
                         (value->text @value))
                       "")
            :class (if @open? "input-active dropdown-active")
            :auto-complete false}]
          (when @open?
            [:div.selectize-dropdown.single
             [renderer results query selected cb opts]])])})))
