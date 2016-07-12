/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rappsilber.xlmod;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 *
 * @author lfischer
 */
public class XLModEntry {
    protected String id;
    protected String name;
    protected HashSet<String> synonyms = new HashSet<String>();
    protected double monoMass;
    protected String specificityString;
    protected int reactionsites;
    protected ArrayList<HashSet<Specificity>>  specificities;

    public static final XLModEntry NO_ENTRY = new XLModEntry();

    protected XLModEntry() {
        
    }
    
    public XLModEntry(String ID, String Name, double MonoMass, String Specificity, int reactionsites) throws ParseException {
        this.id = ID;
        this.name = Name;
        this.monoMass = MonoMass;
        this.specificityString = Specificity.trim();
        this.reactionsites = reactionsites;
        this.specificities = new ArrayList<HashSet<Specificity>>(reactionsites); 
        
        // split the specificties int sites
        String[] sites = specificityString.split("&");
        if (sites.length != reactionsites) {
            throw new ParseException("Specificities do not fit to number of reaction sites", 0);
        }
        
        for (int s = 0;s< sites.length; s++) {
            sites[s]=sites[s].trim();
            sites[s]=sites[s].substring(1,sites[s].length()-1);
            String[] sitespecifies =  sites[s].split(",");
            HashSet<Specificity> hs = new HashSet<Specificity>(sitespecifies.length);
            for (String entry : sitespecifies) {
                hs.add(new Specificity(entry));
            }
            specificities.add(hs);
        }
        
    
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the name
     */
    public HashSet<String> getSynonyms() {
        return synonyms;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the monoMass
     */
    public double getMonoMass() {
        return monoMass;
    }

    /**
     * @param monoMass the monoMass to set
     */
    public void setMonoMass(double monoMass) {
        this.monoMass = monoMass;
    }

    /**
     * @return the specificityString
     */
    public String getSpecificityString() {
        return specificityString;
    }

    /**
     * @param specificityString the specificityString to set
     */
    public void setSpecificityString(String specificityString) {
        this.specificityString = specificityString;
    }

    /**
     * @return the reactionsites
     */
    public int getReactionsites() {
        return reactionsites;
    }

    /**
     * @param reactionsites the reactionsites to set
     */
    public void setReactionsites(int reactionsites) {
        this.reactionsites = reactionsites;
    }

    /**
     * @return the specificities
     */
    public ArrayList<HashSet<Specificity>> getSpecificities() {
        return specificities;
    }

    /**
     * @param specificities the specificities to set
     */
    public void setSpecificities(ArrayList<HashSet<Specificity>> specificities) {
        this.specificities = specificities;
    }
    

}
