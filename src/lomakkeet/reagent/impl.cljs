(ns lomakkeet.reagent.impl
  (:require [reagent.ratom :refer-macros [reaction]]
            [lomakkeet.core :as l]))

(defn cb [form ks value]
  (swap! (:data form) l/change-value ks value (select-keys form [:validation-fn :coercion-matcher])))

(defn blur [form ks]
  ; https://github.com/reagent-project/reagent/issues/135
  (swap! (:data form) update :not-pristine assoc-in ks {}))

(defn get-or-deref [x]
  (if (satisfies? IDeref x) @x x))

;; FORM GROUP ("bootstrap")

(defn default-form-group
  [{:keys [error pristine size label help-text explain-error]
    :or {size 6
         explain-error l/default-explain-error}
    :as opts}
   input]
  [:div.form-group
   {:class (str (if (and (not @pristine) @error) "has-error ")
                (if (and @pristine @error) "needs-attention ")
                (if size (str " col-md-" size " ")))}
   [:label label]
   [input opts]
   (if help-text
     [:span.help-block help-text])
   (if (and (not @pristine) @error)
     [:span.help-block (explain-error @error)])])

;; BASIC INPUTS

(defn input-input [attrs value cb blur]
  [:input.form-control
   (merge attrs
          {:type "text"
           :value (or value "")
           :on-change cb
           :on-blur blur})])

(defn input-password [attrs value cb blur]
  [:input.form-control
   (merge attrs
          {:type "password"
           :value (or value "")
           :on-change cb
           :on-blur blur})])

(defn input-textarea [attrs value cb blur]
  [:textarea.form-control
   (merge attrs
          {:value (or value "")
           :on-change cb
           :on-blur blur})])

(defn input-static [attrs value _ _]
  [:p.form-control-static
   value])

(defn input*
  [{:keys [value cb blur transform-value el attrs]
    :or {transform-value identity
         el input-input}}]
  (el
    attrs
    (transform-value @value)
    #(cb (.. % -target -value))
    #(blur)))

;; CHECKBOX

(defn checkbox*
  [form {:keys [ks]}]
  (let [form-value (reaction (:value @(:data form)))
        value (reaction (get-in @form-value ks))]
    (fn []
      [:input
       {:type "checkbox"
        :checked (boolean @value)
        :on-change #(cb form ks (.. % -target -checked))
        :on-blur #(blur form ks)}])))

;; SELECT

(defn select*
  [form {:keys [ks options attrs]}]
  (let [form-value (reaction (:value @(:data form)))
        value (reaction (get-in @form-value ks))]
    (fn []
      [:select.form-control
       (merge
         (merge (get-or-deref (:attrs form)) attrs)
         {:value @value
          :on-change #(cb form ks (.. % -target -value))
          :on-blur #(blur form ks)})
       (cond
         (map? options)
         (for [[k v] options]
           [:option {:value k :key v} v]))])))
