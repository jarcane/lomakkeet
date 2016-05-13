(ns example.form
  (:require [reagent.core :as r]
            [schema.core :as s]
            [lomakkeet.reagent :as l]
            [devcards.core :as dc :include-macros true]))

(s/defschema SimpleForm
  {:name (s/constrained s/Str seq 'required)
   :email (s/constrained s/Str seq 'required)
   :yes s/Bool
   :postalcode (s/maybe
                 {:code s/Str
                  :name s/Str})})

(defn simple-form' [state]
  (let [form (l/create-form state)]
    (fn [_]
      [:div.container-fluid
       [:div.row
        [l/input form "Name" [:name]]
        [l/input form "Email" [:email]]
        [l/checkbox form "Yes or no" [:yes]]]
       [:div.row
        [l/complete form "Postalcode" [:postalcode] {:item->value #(select-keys % [:code :name])
                                                     :item->text #(str (:code %) " - " (:name %))
                                                     :value->text (fn [_ v] (if v (str (:code v) " - " (:name v))))
                                                     :search-fields [:code :name]
                                                     :items [{:code "33720" :name "Tampere"}
                                                             {:code "33100" :name "Tampere"}]}]]])))

(dc/defcard-rg simple-form
  (fn [state _]
    [simple-form' state])
  (r/atom (l/->fs {:name "Foo" :email "Bar" :postalcode nil} SimpleForm))
  {:inspect-data true})
