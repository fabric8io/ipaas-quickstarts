/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.quickstarts.camel.drools;


/**
 * The Class Person.
 */
public class Person {

    /** The name. */
    private String name;

    /** The likes. */
    private String likes;

    /** The status. */
    private String status;

    /** The age. */
    private int age;

    /** The can drink. */
    private boolean canDrink = false;

    /** The alive. */
    private boolean alive;

    /** The sex. */
    private char sex;

    /** The happy. */
    private boolean happy;

    /**
     * Instantiates a new person.
     */
    public Person() {

    }

    /**
     * Instantiates a new person.
     *
     * @param name the name
     */
    public Person(final String name) {
        this(name, "", 0);
    }

    /**
     * Instantiates a new person.
     *
     * @param name the name
     * @param likes the likes
     */
    public Person(final String name, final String likes) {
        this(name, likes, 0);
    }

    /**
     * Instantiates a new person.
     *
     * @param name the name
     * @param likes the likes
     * @param age the age
     */
    public Person(final String name, final String likes, final int age) {
        this.name = name;
        this.likes = likes;
        this.age = age;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the age.
     *
     * @return the age
     */
    public int getAge() {
        return age;
    }

    /**
     * Sets the age.
     *
     * @param age the new age
     */
    public void setAge(int age) {
        this.age = age;
    }

    /**
     * Checks if is can drink.
     *
     * @return true, if is can drink
     */
    public boolean isCanDrink() {
        return canDrink;
    }

    /**
     * Sets the can drink.
     *
     * @param canDrink the new can drink
     */
    public void setCanDrink(boolean canDrink) {
        this.canDrink = canDrink;
    }

    /**
     * Gets the status.
     *
     * @return the status
     */
    public String getStatus() {
        return this.status;
    }

    /**
     * Sets the status.
     *
     * @param status the new status
     */
    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     * Gets the likes.
     *
     * @return the likes
     */
    public String getLikes() {
        return this.likes;
    }

    /**
     * Checks if is alive.
     *
     * @return true, if is alive
     */
    public boolean isAlive() {
        return this.alive;
    }

    /**
     * Sets the alive.
     *
     * @param alive the new alive
     */
    public void setAlive(final boolean alive) {
        this.alive = alive;
    }

    /**
     * Gets the sex.
     *
     * @return the sex
     */
    public char getSex() {
        return this.sex;
    }

    /**
     * Sets the sex.
     *
     * @param sex the new sex
     */
    public void setSex(final char sex) {
        this.sex = sex;
    }

    @Override
    public String toString() {
        return "Person{" + "name='" + name + '\'' + ", age=" + age + ", canDrink=" + canDrink + '}';
    }

    /**
     * Checks if is happy.
     *
     * @return true, if is happy
     */
    public boolean isHappy() {
        return happy;
    }

    /**
     * Sets the happy.
     *
     * @param happy the new happy
     */
    public void setHappy(boolean happy) {
        this.happy = happy;
    }
}
