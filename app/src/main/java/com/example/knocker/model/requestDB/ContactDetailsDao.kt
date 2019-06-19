package com.example.knocker.model.requestDB

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.knocker.model.ModelDB.ContactDetailDB

/**
 * Interface réunissent les différentes requêtes d'interaction avec la table contact detail
 * @author Florian Striebel, Ryan Granet
 */
@Dao
interface ContactDetailsDao {
    /**
     * Récupère un [contact detail][ContactDetailDB] qui possède un numéro de téléphone grâce à son id.
     * @param id Int    Id du contact sélectionné
     * @return [ContactDetailDB]
     */
    @Query("SELECT * FROM contact_details_table where tag='phone'AND id_contact=:id")
    fun getPhoneNumberById(id:Int?): ContactDetailDB
    /**
     * Récupère un [contact detail][ContactDetailDB] qui possède une addresse mail grâce à son id.
     * @param id Int    Id du contact sélectionné
     * @return [ContactDetailDB]
     */
    @Query("SELECT * FROM contact_details_table where tag='mail'AND id_contact=:id")
    fun getMailById(id:Int?): ContactDetailDB
    /**
     * Récupère tout les [contacts details][ContactDetailDB] de la table.
     * @return List&lt[ContactDetailDB]&gt
     */
    @Query("SELECT * FROM contact_details_table")
    fun getAllpropertiesEditContact():List<ContactDetailDB>
    /**
     * Récupère tout les [contacts details][ContactDetailDB] que possède un contact mail grâce à son id.
     * @param contactID Int     Id du contact sélectionné
     * @return List&lt[ContactDetailDB]&gt
     */
    @Query("SELECT * FROM contact_details_table WHERE id_contact=:contactID")
    fun getDetailsForAContact(contactID:Int):List<ContactDetailDB>
    /**
     * Update un [contacts details][ContactDetailDB] grâce à son id.
     * @param id Int            Id du contact sélectionné
     * @param contactDetail     détail du contact (mail, numéro de tel, etc...)
     */
    @Query("UPDATE contact_details_table SET content = :contactDetail WHERE id = :id")
    fun updateContactDetailById(id: Int, contactDetail: String)
    /**
     * Ajoute un [contact detail][ContactDetailDB] dans la base de données.
     * @param ContactDetailDB contactDetailDB    Objet [contact detail][ContactDetailDB]
     */
    @Insert
    fun insert(contactDetailDB: ContactDetailDB)
    /**
     * Supprime un [contact detail][ContactDetailDB] dans la base de données.
     * @param id Int    id du detail
     */
    @Query("DELETE FROM contact_details_table WHERE id = :id")
    fun deleteDetailById(id: Int)
}