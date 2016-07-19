package com.specknet.airrespeck.datamodels;


import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;


@Table(name = "User")
public class User extends Model {

    @Column(name = "UniqueId")
    @SerializedName("unique_id")
    @Expose
    private String uniqueId;

    @Column(name = "FirstName")
    @SerializedName("first_name")
    @Expose
    private String firstName;

    @Column(name = "LastName")
    @SerializedName("last_name")
    @Expose
    private String lastName;

    @Column(name = "BirthDate")
    @SerializedName("birth_date")
    @Expose
    private Date birthDate;

    @Column(name = "Gender")
    @SerializedName("gender")
    @Expose
    private String gender;

    @Column(name = "Usertype")
    @SerializedName("usertype")
    @Expose
    private int usertype;

    @Column(name = "Illiterate")
    @SerializedName("illiterate")
    @Expose
    private boolean illiterate;

    @Column(name = "Active")
    @SerializedName("active")
    @Expose
    private boolean active;

    /**
     * No args constructor for use in serialization
     *
     */
    public User() {
        // Active Android SQLite
        super();
    }

    /**
     *
     * @param uniqueId
     * @param firstName
     * @param lastName
     * @param birthDate
     * @param gender
     * @param usertype
     * @param illiterate
     */
    public User(String uniqueId, String firstName, String lastName, Date birthDate, String gender, int usertype, boolean illiterate) {
        // Active Android SQLite
        super();

        this.uniqueId = uniqueId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.gender = gender;
        this.usertype = usertype;
        this.illiterate = illiterate;
        this.active = true;
    }

    /**
     *
     * @return
     * The uniqueId
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     *
     * @param uniqueId
     * The unique_id
     */
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     *
     * @return
     * The firstName
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     *
     * @param firstName
     * The first_name
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     *
     * @return
     * The lastName
     */
    public String getLastName() {
        return lastName;
    }

    /**
     *
     * @param lastName
     * The last_name
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     *
     * @return
     * The birthDate
     */
    public Date getBirthDate() {
        return birthDate;
    }

    /**
     *
     * @param birthDate
     * The birth_date
     */
    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    /**
     *
     * @return
     * The gender
     */
    public String getGender() {
        return gender;
    }

    /**
     *
     * @param gender
     * The gender
     */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /**
     *
     * @return
     * The usertype
     */
    public int getUserType() {
        return usertype;
    }

    /**
     *
     * @param usertype
     * The usertype
     */
    public void setUserType(int usertype) {
        this.usertype = usertype;
    }

    /**
     *
     * @return
     * The illiterate
     */
    public boolean isIlliterate() {
        return illiterate;
    }

    /**
     *
     * @param illiterate
     * The illiterate
     */
    public void setIlliterate(boolean illiterate) {
        this.illiterate = illiterate;
    }

    /**
     *
     * @return
     * The active
     */
    public boolean isActive() {
        return active;
    }

    /**
     *
     * @param active
     * The active
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return ("User: {local_id=" + getId() +
                ", unique_id=" + uniqueId +
                ", first_name=" + firstName +
                ", last_name=" + lastName +
                ", birth_date=" + birthDate +
                ", gender=" + gender +
                ", usertype=" + usertype +
                ", illiterate=" + illiterate +
                ", active=" + active +
                "}");
    }


    /**************************************************
     * STATIC METHODS TO MANIPULATE THE USER DATABASE
     **************************************************/

    /**
     * Check whether the table User is empty or not.
     * @return boolean True if the table has records, else false.
     */
    public static boolean isTableEmpty() {
        List<User> users = new Select()
                .from(User.class)
                .execute();

        return users.isEmpty();
    }

    /**
     * Get user (the current/only one) from the table User.
     * @return User The first record in the table User.
     */
    public static User getUser() {
        return new Select()
                .from(User.class)
                .executeSingle();
    }

    /**
     * Get user by its unique id.
     * @param uniqueId String User unique id.
     * @return User A user record.
     */
    public static User getUserByUniqueId(final String uniqueId) {
        return new Select()
                .from(User.class)
                .where("uniqueId = ?", uniqueId)
                .executeSingle();
    }

    /**
     * Delete a user by its unique id.
     * @param uniqueId String User unique id of the record to be deleted.
     */
    public static void deleteUserByUniqueId(final String uniqueId) {
        new Delete()
                .from(User.class)
                .where("uniqueId = ?", uniqueId)
                .execute();
    }
}