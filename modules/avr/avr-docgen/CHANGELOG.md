# CHANGE LOG

## v4.1.6 - 2022-04-12 - init changelog

**Added:**
* Envoi de mail avec pièce-jointe
* Génération de pdf depuis un template word (Configuré dans la configuration docgen dans Axelor)
* Gestion des factures fournisseur et avoir fournisseur (+ configuration module)
* Configuration docgen par champs et bindé par le titre dans le tempalte word
* Communication avec le serveur docgen
* Possibilité de tester son template avec un bouton dans le docgen template
* Exportation / Importation des configurations docgen (dans le menu docgen templates)
* Multi-langue supportée (récupère la langue du client à qui on souhaite l'envoyer)
* Possibilité de modifier l'ordonnance de récupèration des éléments (lorsque c'est une liste) + Ascendant / Descendant
* Possibilité d'envoi d'image 
* Possibilité d'envoi d'HTML (Attention aspose gère mal beaucoup d'élément HTML)
* Choix du nombre de décimal à récupéré.
* Possibilité de faire du HQL et du SQL pour la récupération d'élément.

**Fixed:**
- Fix problem of name changed for dependencies : `avr-docgen` --> `docgen`
- Check if partner is null if is required when generate file