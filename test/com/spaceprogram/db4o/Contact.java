package com.spaceprogram.db4o;


/**
 * User: Travis Reeder
 * Date: May 18, 2006
 * Time: 5:48:04 PM
 */
public class Contact  {
    private Integer id;
    private String name;
    private String email;
    private int age;
    private String category;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "[" + id + "] " + name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
