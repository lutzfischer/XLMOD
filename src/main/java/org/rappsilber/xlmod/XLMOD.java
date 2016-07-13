/*
 * Copyright 2015 Lutz Fischer <lfischer at staffmail.ed.ac.uk>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rappsilber.xlmod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.rappsilber.data.csv.CSVRandomAccess;

/**
 * XLModList
 * @author lfischer
 */
public class XLMOD implements Iterable<XLModEntry>   {
//    public static String XLMOD_URL = "https://raw.githubusercontent.com/HUPO-PSI/mzIdentML/master/cv/XLMOD-1.0.0.csv";
    public static String XLMOD_URL = "https://raw.githubusercontent.com/HUPO-PSI/mzIdentML/master/cv/XLMOD.obo";
    
    TreeMap<Long,ArrayList<XLModEntry>> massToMod = new TreeMap<Long, ArrayList<XLModEntry>>();
    HashMap<String,XLModEntry> idToMod = new HashMap<String,XLModEntry>();
    HashMap<Integer,ArrayList<XLModEntry>> byReactionSites = new HashMap<Integer, ArrayList<XLModEntry>>();
    
    ArrayList<XLModEntry> list;
    
    public void read() throws IOException, ParseException {
        read(XLMOD_URL) ;
    }

    public void read(String source) throws IOException, ParseException { 
        list = new ArrayList<XLModEntry>();
        if (source.endsWith(".obo")) {
            readObo(source);
        } else {
            readCSV(source);
        }
    }
    
    
    
    public void readCSV(String source) throws IOException, ParseException {
        java.net.URI moduri=null;
        try {
             moduri = new java.net.URI(source);
        } catch (URISyntaxException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        
        CSVRandomAccess XLMOD_csv = new CSVRandomAccess(',','\"');
        XLMOD_csv.openURI(moduri, true);
        while (XLMOD_csv.next()) {
            double mass = XLMOD_csv.getDouble("monoisotopicMass");
            String name = XLMOD_csv.getValue("name");
            String id = XLMOD_csv.getValue("id");
            String specificity = XLMOD_csv.getValue("specificities");
            int sites = (int)XLMOD_csv.getInteger("reactionSites");
            if (!id.isEmpty()) {
                add(new XLModEntry(id, name, mass, specificity, sites));
            }
        }
        
    }
    
    public void readObo(String source) throws IOException, ParseException {
        java.net.URI moduri=null;
        try {
             moduri = new java.net.URI(source);
        } catch (URISyntaxException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        BufferedReader in = null;
        

        OBOFormatParser obop = new OBOFormatParser();
//        obop.
        obop.setReader(in);
        if (moduri.getScheme().equals("file")) {
            in = new BufferedReader(new FileReader(new File(moduri)));
        } else {
            URL url = new URL(moduri.toString());
            URLConnection connection = url.openConnection();
            InputStream is = connection.getInputStream();    
            in = new BufferedReader(new InputStreamReader(is));
        }
        
        OBODoc obo = obop.parse(in);
        
        Collection<org.obolibrary.oboformat.model.Frame> frames = obo.getTermFrames();
        
        for (org.obolibrary.oboformat.model.Frame f : frames) {
            String id = f.getId();
            String name = f.getTagValue("name",String.class);
            
            List<Clause> l = f.getClauses(OboFormatTag.TAG_PROPERTY_VALUE);
            Double mass = null;
            String specificity = null;
            Integer sites = null;
            for (Clause c : l) {
                String data = c.getValue(String.class);
                if (data.contentEquals("monoisotopicMass:")) {
                    mass = Double.valueOf(c.getValue2(String.class));
                } else if (data.contentEquals("specificities:")) {
                    specificity = c.getValue2(String.class);
                } else if (data.contentEquals("reactionSites:")) {
                    sites = Integer.valueOf(c.getValue2(String.class));
                }
            }
            // skip everything that does not have a mass as a non-modification entry
            if (mass == null) {
                continue;
            }

            XLModEntry e =new XLModEntry(id, name, mass, specificity, sites);
            add(e);
            
            // get synonymes
            l = f.getClauses(OboFormatTag.TAG_SYNONYM);
            for (Clause c : l) {
                String syn = c.getValue(String.class);
                e.getSynonyms().add(syn);
            }
            
        }
        
    }
    
    
    public boolean add(XLModEntry entry) {
        if (list.add(entry)) {
            idToMod.put(entry.id, entry);
            long key= Math.round(entry.getMonoMass()*1000);
            ArrayList<XLModEntry> re = byReactionSites.get(entry.reactionsites);
            if (re == null) {
                re = new ArrayList<XLModEntry>();
                byReactionSites.put(entry.reactionsites, re);
            }
            re.add(entry);
            
            
            ArrayList<XLModEntry> prev = massToMod.get(key);
            if (prev == null) {
                prev = new ArrayList<XLModEntry>();
                massToMod.put(key, prev);
            }
            if (prev.add(entry)) {
                return true;
            } else {
                list.remove(entry);
                return false;
            }
        }
        return false;
    }
    
    protected boolean matches(Specificity s, String linkedResidues,  boolean isTerm, boolean isProtNterm, boolean isProtCTerm, boolean isPepNterm, boolean isPepCterm) {
        if (s.terminalRestricted )  {
            if (isTerm && ((s.PeptideCterminal && isPepCterm) || 
                           (s.PeptideNterminal && isPepNterm) ||
                           (s.ProteinCterminal && isProtCTerm) ||
                           (s.ProteinNterminal && isProtNterm)
                    )) {
                if (s.isNonSpecific)
                    return true;

                return s.AminoAcid.contentEquals(linkedResidues);
            }
            
            return false;
            
        }
        return s.AminoAcid.contentEquals(linkedResidues);
        
    }
    
    public XLModEntry guessModification(double mass, String[] linkedResidues, boolean[] isProtNterm, boolean[] isProtCTerm, boolean[] isPepNterm, boolean[] isPepCterm) {
        if (linkedResidues.length >2) {
            throw new UnsupportedOperationException("Currently can only guess up to dimeric cross-linker");
        }
        long key=Math.round(mass*1000);
        ArrayList<XLModEntry> candidates =  massToMod.get(key);
        // do we have something with that mass?
        if (candidates == null) {
            return null;
        }
        
        // does any of the candidates match?
        for (XLModEntry e : candidates) {
            if (e.getReactionsites() == linkedResidues.length)  {
                if (e.getReactionsites() == 1) {
                    boolean isTerm = isProtNterm[0] || isProtCTerm[0] || isPepNterm[0] || isPepCterm[0];
                    for (Specificity s : e.getSpecificities().get(0)) {
                        if (matches(s, linkedResidues[0], isTerm, isProtNterm[0] , isProtCTerm[0] , isPepNterm[0] , isPepCterm[0])) {
                            return e;
                        }
                    }
                } else {
                    boolean matches = true;
                    for (int i = 0; i<linkedResidues.length;i++) {
                        boolean siteMatches = false;
                        boolean isTerm = isProtNterm[i] || isProtCTerm[i] || isPepNterm[i] || isPepCterm[i];
                        for (Specificity s : e.getSpecificities().get(0)) {
                            if (matches(s, linkedResidues[i], isTerm, isProtNterm[i] , isProtCTerm[i] , isPepNterm[i] , isPepCterm[i])) {
                                siteMatches = true;
                                break;
                            }
                        }
                        if (!siteMatches) {
                            matches = false;
                            break;
                        }
                    }
                    if (!matches) {
                        matches = true;
                        for (int i = 0; i<linkedResidues.length;i++) {
                            int v = linkedResidues.length-i-1;
                            boolean siteMatches = false;
                            boolean isTerm = isProtNterm[v] || isProtCTerm[v] || isPepNterm[v] || isPepCterm[v];
                            for (Specificity s : e.getSpecificities().get(0)) {
                                if (matches(s, linkedResidues[i], isTerm, isProtNterm[i] , isProtCTerm[i] , isPepNterm[i] , isPepCterm[i])) {
                                    siteMatches = true;
                                    break;
                                }
                            }
                            if (!siteMatches) {
                                matches = false;
                                break;
                            }
                        }
                    }
                    if (matches) {
                        return e;
                    }
                }
            }
        }
        return null;
    }


    public XLModEntry guessModification(double mass, String name, String[] linkedResidues, boolean[] isProtNterm, boolean[] isProtCTerm, boolean[] isPepNterm, boolean[] isPepCterm) {
        if (linkedResidues.length >2) {
            throw new UnsupportedOperationException("Currently can only guess up to dimeric cross-linker");
        }
        long key=Math.round(mass*1000);
        ArrayList<XLModEntry> massCandidates =  massToMod.get(key);
        // do we have something with that mass?
        if (massCandidates == null) {
            return null;
        }
        
        
        ArrayList<XLModEntry> candidates = new ArrayList<XLModEntry>();
        
        // does any of the candidates match?
        for (XLModEntry e : massCandidates) {
            if (e.getReactionsites() == linkedResidues.length)  {
                if (e.getReactionsites() == 1) {
                    boolean isTerm = isProtNterm[0] || isProtCTerm[0] || isPepNterm[0] || isPepCterm[0];
                    for (Specificity s : e.getSpecificities().get(0)) {
                        if (matches(s, linkedResidues[0], isTerm, isProtNterm[0] , isProtCTerm[0] , isPepNterm[0] , isPepCterm[0])) {
                            if (e.getName().toLowerCase().equals(name.toLowerCase()) || e.getName().substring(6).toLowerCase().equals(name.toLowerCase())) {
                                return e;
                            }
                            candidates.add(e);
                            break;
                        }
                    }
                } else {
                    boolean matches = true;
                    for (int i = 0; i<linkedResidues.length;i++) {
                        boolean siteMatches = false;
                        boolean isTerm = isProtNterm[i] || isProtCTerm[i] || isPepNterm[i] || isPepCterm[i];
                        for (Specificity s : e.getSpecificities().get(0)) {
                            if (matches(s, linkedResidues[i], isTerm, isProtNterm[i] , isProtCTerm[i] , isPepNterm[i] , isPepCterm[i])) {
                                siteMatches = true;
                                break;
                            }
                        }
                        if (!siteMatches) {
                            matches = false;
                            break;
                        }
                    }
                    if (!matches) {
                        matches = true;
                        for (int i = 0; i<linkedResidues.length;i++) {
                            int v = linkedResidues.length-i-1;
                            boolean siteMatches = false;
                            boolean isTerm = isProtNterm[v] || isProtCTerm[v] || isPepNterm[v] || isPepCterm[v];
                            for (Specificity s : e.getSpecificities().get(0)) {
                                if (matches(s, linkedResidues[i], isTerm, isProtNterm[i] , isProtCTerm[i] , isPepNterm[i] , isPepCterm[i])) {
                                    siteMatches = true;
                                    break;
                                }
                            }
                            if (!siteMatches) {
                                matches = false;
                                break;
                            }
                        }
                    }
                    if (e.getName().toLowerCase().equals(name.toLowerCase()) || (e.getName().startsWith("Xlink:") && e.getName().substring(6).toLowerCase().equals(name.toLowerCase()))) {
                        return e;
                    }
                    candidates.add(e);
                }
            }
            
        }
        //String queryName = name.replaceAll("(.*:)?([a-zA-Z0-9]+)([\\-_]?d[0-9]+)?(!.*)?", "$2");
        String queryName = name.replaceAll("(oh2?|nh[23]|loop)$", "");
        queryName = queryName.toLowerCase().replaceAll("(.*:)?([a-zA-Z0-9]+)([-_]?d[0-9]+)?", "$2");
        // check synonymes for exact matches
        for (XLModEntry c : candidates) {
            for (String s: c.getSynonyms()) {
                if (s.contentEquals(queryName)) {
                    return c;
                }
            }
        }
        
        // check the names and synonymes of the candidates for containing the name
        for (XLModEntry c : candidates) {
            if (c.getName().toLowerCase().contains(queryName)) {
                return c;
            }
            for (String s: c.getSynonyms()) {
                if (s.contains(queryName)) {
                    return c;
                }
            }
        }
        return null;
    }
    
    public XLModEntry guessModification(XLModQuery q) {
        if (q.name != null && !q.name.isEmpty())
            return guessModification(q.mass, q.name, q.linkedResidues, q.isProtNterm, q.isProtCTerm, q.isPepNterm, q.isPepCterm);
        return guessModification(q.mass, q.linkedResidues, q.isProtNterm, q.isProtCTerm, q.isPepNterm, q.isPepCterm);
    }
 
    HashMap<XLModQuery, XLModEntry> queryCache = new HashMap<XLModQuery, XLModEntry>();
    public XLModEntry guessModificationCached(XLModQuery q) {
        XLModEntry e = queryCache.get(q);
        if (e == XLModEntry.NO_ENTRY) {
            return null;
        }
        if (e == null) {
            e = guessModification(q);
            if (e == null) {
                queryCache.put(q,XLModEntry.NO_ENTRY);
            } else {
                queryCache.put(q, e);
            }
            return e;
        }
        return e;
    }
    
    public XLModEntry get(String id) {
        return idToMod.get(id);
    }

    public Iterator<XLModEntry> iterator() {
        return list.iterator();
    }
    
    
    
}
