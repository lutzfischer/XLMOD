/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rappsilber.xlmod;

import java.util.Arrays;

/**
 *
 * @author lfischer
 */
public class XLModQuery {
    double mass;
    String[] linkedResidues;
    boolean[] isProtNterm;
    boolean[] isProtCTerm;
    boolean[] isPepNterm;
    boolean[] isPepCterm;
    String name = null;
    boolean isTerminal;
    int hashcode;

    public XLModQuery(double mass, String[] linkedResidues, boolean[] isProtNterm, boolean[] isProtCTerm, boolean[] isPepNterm, boolean[] isPepCterm) {
        this.mass = mass;
        this.linkedResidues = linkedResidues;
        this.isProtNterm = isProtNterm;
        this.isProtCTerm = isProtCTerm;
        this.isPepNterm = isPepNterm;
        this.isPepCterm = isPepCterm;
        hashcode = (int)mass;
        for (int i = 0 ; i<linkedResidues.length;i++) {
            hashcode += linkedResidues[i].hashCode();
            boolean term = isProtNterm[i] || isProtCTerm[i] || isPepNterm[i] || isPepCterm[i];
            hashcode += Boolean.hashCode(term);
            isTerminal |= term;
        }
    }
    public XLModQuery(double mass, String name,String[] linkedResidues, boolean[] isProtNterm, boolean[] isProtCTerm, boolean[] isPepNterm, boolean[] isPepCterm) {
        this(mass, linkedResidues, isProtNterm, isProtCTerm, isPepNterm, isPepCterm);
        this.name = name;
    }

    @Override
    public int hashCode() {
        return hashcode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
       
        if (getClass() != obj.getClass()) {
            return false;
        }
        final XLModQuery other = (XLModQuery) obj;
        
        if (other.hashcode != hashcode) 
            return false;
        
        
        if (Double.doubleToLongBits(this.mass) != Double.doubleToLongBits(other.mass)) {
            return false;
        }
        if (!Arrays.deepEquals(this.linkedResidues, other.linkedResidues)) {
            return false;
        }

        if (!(this.name == other.name || (this.name!=null && this.name.equals(other.name)))) {
            return false;
        }
        
        if (isTerminal) {
            if (!other.isTerminal)
                return false;
            
            if (!Arrays.equals(this.isProtNterm, other.isProtNterm)) {
                return false;
            }
            if (!Arrays.equals(this.isProtCTerm, other.isProtCTerm)) {
                return false;
            }
            if (!Arrays.equals(this.isPepNterm, other.isPepNterm)) {
                return false;
            }
            if (!Arrays.equals(this.isPepCterm, other.isPepCterm)) {
                return false;
            }
        }
        return true;
    }
    
    
    
}
