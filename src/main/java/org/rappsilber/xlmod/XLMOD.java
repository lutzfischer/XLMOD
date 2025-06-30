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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.model.Xref;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.obolibrary.oboformat.parser.OBOFormatParser;
import org.rappsilber.data.csv.CSVRandomAccess;
import org.rappsilber.utils.RArrayUtils;

/**
 * XLMOD parsing and guessing of modifications.
 * 
 * #TODO XLMOD also contains DNA/RNA crosslinker - currently parsed as reacting to anything
 * @author lfischer
 */
public class XLMOD implements Iterable<XLModEntry>   {
//    public static String XLMOD_URL = "https://raw.githubusercontent.com/HUPO-PSI/mzIdentML/master/cv/XLMOD-1.0.0.csv";
    public static String XLMOD_URL = "https://raw.githubusercontent.com/HUPO-PSI/mzIdentML/master/cv/XLMOD.obo";
    public static String XLMOD_LOCAL = "resource:///XLMOD.obo";
    
    TreeMap<Long,ArrayList<XLModEntry>> massToMod = new TreeMap<Long, ArrayList<XLModEntry>>();
    ArrayList<XLModEntry> nullMassMods = new ArrayList<>();
    HashMap<String,XLModEntry> nameToMod = new HashMap<String, XLModEntry>();
    HashMap<String,XLModEntry> idToMod = new HashMap<String,XLModEntry>();
    HashMap<Integer,ArrayList<XLModEntry>> byReactionSites = new HashMap<Integer, ArrayList<XLModEntry>>();
    
    ArrayList<XLModEntry> list;
    protected String usedURL;
    /**
     * Each mass is multiplied by this factor and cast to integer.
     * Therefor this encodes the tolerance on how masses are matched.
     */
    protected int factor = 10000;
    
    
    /**
     * reads in the bundled XLMOD.obo
     * @throws IOException
     * @throws ParseException 
     */
    public void read() throws IOException, ParseException {
        read(true);
    }

    /**
     * reads in the obo-file from it's default location
     * @throws IOException
     * @throws ParseException 
     */
    public void read(boolean local) throws IOException, ParseException {
        if (!local) {
            try {
                read(XLMOD_URL) ;
            } catch ( Exception e ) {
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"failed to load remod obo-file - falling back to embeded one",e);
                read(XLMOD_LOCAL) ;
            }
        } else {
            read(XLMOD_LOCAL) ;
        }
    }
    
    /**
     * reads in XLMOD in either the csv or the obo-format.
     * It can either read from a file or from an url.
     * @param source from where to read.
     * @throws IOException
     * @throws ParseException 
     */
    public void read(String source) throws IOException, ParseException { 
        setUsedURL(source);
        list = new ArrayList<XLModEntry>();
        if (source.endsWith(".obo")) {
            readObo(source);
        } else {
            readCSV(source);
        }
    }
    
    
    /**
     * reads in XLMOD in the csv-format.
     * It can either read from a file or from an url.
     * @param source from where to read.
     * @throws IOException
     * @throws ParseException 
     */
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
                add(new XLModEntry(id, name, mass, specificity, sites, null));
            }
        }
        
    }
    
    XLModEntry getEntryFromFrame(Frame f, OBODoc d, Double defaultMass) throws ParseException {
        String id = f.getId();
        String name = f.getTagValue("name",String.class);

        List<Clause> l = f.getClauses(OboFormatTag.TAG_PROPERTY_VALUE);
        Double mass = defaultMass;
        String specificity = "";
        Integer sites = null; // asume modification
        for (Clause c : l) {
            String data = c.getValue(String.class);
            if (data.toLowerCase().contentEquals("monoisotopicmass:")) {
                mass = Double.valueOf(c.getValue2(String.class));
            } else if (data.toLowerCase().contentEquals("specificities:")) {
                specificity = c.getValue2(String.class);
            } else if (data.toLowerCase().contentEquals("reactionsites:")) {
                sites = Integer.valueOf(c.getValue2(String.class));
            }
        }
        ArrayList<String> synonyms = new ArrayList<>();
        List<Clause> sl = f.getClauses(OboFormatTag.TAG_SYNONYM);
        for (Clause c : sl) {
            synonyms.add(c.getValue(String.class));
        }
        
        //if (mass == null)
        //    return null;
        StringBuffer sbSpecificity = new StringBuffer();
        int reactiveGroupsParsed = 0;
        ArrayList<String>  reactive_group_ids = new ArrayList<>();
        int parseReactiveGroups = 1;
        for (Clause c : f.getClauses(OboFormatTag.TAG_IS_A)) {
            if (c.getValue(String.class).contentEquals("XLMOD:00006"))
                parseReactiveGroups = 2;
        }
        RELATIONSHIP: for (Clause c : f.getClauses(OboFormatTag.TAG_RELATIONSHIP)) {
            //if (name.contentEquals("SDA"))  {
            if (c.getValue().toString().contentEquals("has_reactive_group") && specificity.length()==0) {
                reactiveGroupsParsed ++;
                sbSpecificity.append("&(");
                // find the reactive group and parse
                String group = c.getValue2().toString();
                Frame rgf = d.getTermFrame(group);
                for (Clause rc : rgf.getClauses(OboFormatTag.TAG_RELATIONSHIP)) {
                    if (rc.getValue(String.class).contentEquals("is_reactive_with") &&  rc.getValue2(String.class).contentEquals("XLMOD:00037")) {
                        sbSpecificity.append("ANY)");
                        continue RELATIONSHIP;
                    }
                }
                List<Clause> rgfcl = rgf.getClauses(OboFormatTag.TAG_PROPERTY_VALUE);
                // is it unspecific?
                StringBuffer sbThisSpecificity = new StringBuffer();
                for (Clause s : rgfcl) {
                    String data = s.getValue(String.class);
                    if (data.toLowerCase().contentEquals("specificities:")) {
                        String v = s.getValue2(String.class);
                        sbThisSpecificity.append(",").append(v.substring(1,v.length()-1));
                    } else if (data.toLowerCase().contentEquals("secondaryspecificities:")) {
                        String v = s.getValue2(String.class);
                        sbThisSpecificity.append(",").append(v.substring(1,v.length()-1));
                    }
                }
                if (sbThisSpecificity.length() == 0) {
                    // there are some odd crosslinker defintions that I just can't cover here - sorry
                    sbSpecificity.append("Any)");
                } else {
                    sbSpecificity.append(sbThisSpecificity.substring(1)).append(")");
                }
            } else if (c.getValue().toString().contentEquals("is_side_product_of")) {
                // seems this is a partially quenched or not reacted crosslinker mod
                XLModEntry be = getEntryFromFrame(d.getTermFrame(c.getValue2(String.class)), d, 0d);
                
                sites = 1;
                HashSet<String> speSet= new HashSet<>(RArrayUtils.toCollection(be.specificityString.replace(")&(", ",").replace("(","").replace(")","").split(",")));
                specificity = "(" + RArrayUtils.toString(speSet, ",") + ")";
            }
            if (parseReactiveGroups == reactiveGroupsParsed) {
                break;
            }

        }
        if (sites == null && mass == null) {
            return null;
        }
        if (reactiveGroupsParsed >=1) {
            if (reactiveGroupsParsed < sites && !sbSpecificity.substring(1).contains("&")) {
                sbSpecificity.append(sbSpecificity);
            }
            specificity = sbSpecificity.substring(1, sbSpecificity.length());
        }
        sites = (sites == null? 1 : (sites>1?2:1));
        if (specificity.length() == 0) {
            for (int i = 0; i < sites; i++) {
                specificity += "&(ANY)";
            }
            specificity = specificity.substring(1);
        }
        try {
            return new XLModEntry(id, name, mass, specificity, sites , reactive_group_ids);
        } catch (Exception e) {
            return null;
        }

        //return new XLModEntry(id, name, mass, specificity, sites>1?2:1 , reactive_group_ids);
        
    }
    
    /**
     * reads in XLMOD in obo-format.
     * It can either read from a file or from an url.
     * @param source from where to read.
     * @throws IOException
     * @throws ParseException 
     */
    public void readObo(String source) throws IOException, ParseException {
        java.net.URI moduri=null;
        try {
             moduri = new java.net.URI(source);
        } catch (URISyntaxException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        BufferedReader in = null;
        

        OBOFormatParser obop = new OBOFormatParser();
//        obop.setReader(in);
        if (moduri.getScheme().equals("file")) {
            in = new BufferedReader(new FileReader(new File(moduri)));
        } else if (moduri.getScheme().equals("resource")) {
            InputStream resin = getClass().getResourceAsStream(moduri.getPath());
            in = new BufferedReader(new  InputStreamReader(resin));
        } else {
            URL url = new URL(moduri.toString());
            URLConnection connection = url.openConnection();
            InputStream is = connection.getInputStream();    
            in = new BufferedReader(new InputStreamReader(is));
        }
        
        OBODoc obo = obop.parse(in);
        
        Collection<org.obolibrary.oboformat.model.Frame> frames = obo.getTermFrames();
        
        for (org.obolibrary.oboformat.model.Frame f : frames) {
            XLModEntry e = getEntryFromFrame(f, obo, null);
            if (e != null) {
                add(e);
            }
        }
        
    }
    
    /**
     * adds a new {@link XLModEntry} to the list.
     * @param entry
     * @return 
     */
    public boolean add(XLModEntry entry) {
        if (list.add(entry)) {
            idToMod.put(entry.id, entry);
            Long key= (entry.getMonoMass() ==null ? (Long)null: Long.valueOf(Math.round(entry.getMonoMass()*1000)));
            ArrayList<XLModEntry> re = byReactionSites.get(entry.reactionsites);
            if (re == null) {
                re = new ArrayList<XLModEntry>();
                byReactionSites.put(entry.reactionsites, re);
            }
            re.add(entry);
            
            if (key == null) {
                nullMassMods.add(entry);
            } else {
                ArrayList<XLModEntry> prev = massToMod.get(key);
                if (prev == null) {
                    prev = new ArrayList<XLModEntry>();
                    massToMod.put(key, prev);
                }
                if (prev.add(entry)) {
                    nameToMod.put(entry.name, entry);
                    for (String s : entry.synonyms) {
                        nameToMod.put(s, entry);
                    }
                    return true;
                } else {
                    list.remove(entry);
                    return false;
                }
            }
        }
        return false;
    }
    
    /**
     * tests whether the specificity definition would cover the given position.
     * @param s the definition of specificity
     * @param linkedResidues 
     * @param isTerm
     * @param isProtNterm
     * @param isProtCTerm
     * @param isPepNterm
     * @param isPepCterm
     * @return 
     */
    protected boolean matches(XLModSpecificity s, String linkedResidues,  boolean isTerm, boolean isProtNterm, boolean isProtCTerm, boolean isPepNterm, boolean isPepCterm) {
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
        if (s.isNonSpecific)
            return true;        
        return s.AminoAcid.contentEquals(linkedResidues);
        
    }
    
    /**
     * tries to return a XLModEntry that fits to the given set of details.
     * The arguments are arrays with as many entries as there are modification sites.
     * Meaning here if it is  a bi-functional cross-linker each array would hold 
     * two values - one each site of the cross-linker
     * for normal PTMS each array would contain 1 entry
     * @param mass
     * @param linkedResidues
     * @param isProtNterm
     * @param isProtCTerm
     * @param isPepNterm
     * @param isPepCterm
     * @return the first xlmod-entry it find that fits the definition or null if none can be found
     */
    public XLModEntry guessModification(Double mass, String[] linkedResidues, boolean[] isProtNterm, boolean[] isProtCTerm, boolean[] isPepNterm, boolean[] isPepCterm) {
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
                    for (XLModSpecificity s : e.getSpecificities().get(0)) {
                        if (matches(s, linkedResidues[0], isTerm, isProtNterm[0] , isProtCTerm[0] , isPepNterm[0] , isPepCterm[0])) {
                            return e;
                        }
                    }
                } else {
                    boolean matches = true;
                    for (int i = 0; i<linkedResidues.length;i++) {
                        boolean siteMatches = false;
                        boolean isTerm = isProtNterm[i] || isProtCTerm[i] || isPepNterm[i] || isPepCterm[i];
                        for (XLModSpecificity s : e.getSpecificities().get(0)) {
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
                            for (XLModSpecificity s : e.getSpecificities().get(0)) {
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


    /**
     * tries to return a XLModEntry that fits to the given set of details.
     * The arguments are arrays with as many entries as there are modification sites.
     * Meaning here if it is  a bi-functional cross-linker each array would hold 
     * two values - one each site of the cross-linker
     * for normal PTMS each array would contain 1 entry
     * For the name it either returns the first entry where the rest of the 
     * definition fits or if no exact match is found it will then check synonyms 
     * If still no match is found it will look for a substring match first in 
     * name and then in synonyms.
     * @param mass
     * @param linkedResidues
     * @param isProtNterm
     * @param isProtCTerm
     * @param isPepNterm
     * @param isPepCterm
     * @return a xlmod-entry that fits the definition or null if none can be found
     */
    public XLModEntry guessModification(Double mass, String name, String[] linkedResidues, boolean[] isProtNterm, boolean[] isProtCTerm, boolean[] isPepNterm, boolean[] isPepCterm, boolean includeNullMass) {
        if (linkedResidues.length >2) {
            throw new UnsupportedOperationException("Currently can only guess up to dimeric cross-linker");
        }
        ArrayList<XLModEntry> massCandidates;
        if (mass == null) {
            massCandidates =  new ArrayList<>(nullMassMods);
        } else {
            long key=Math.round(mass*1000);
            massCandidates =  massToMod.get(key);
        }
        // do we have something with that mass?
        if (massCandidates == null) {
            if (includeNullMass && name != null && name.length()>0) {
                return guessModification(null, name, linkedResidues, isProtNterm, isProtCTerm, isPepNterm, isPepCterm, false);
            } else
                return null;
        }
        
        
        ArrayList<XLModEntry> candidates = new ArrayList<XLModEntry>();
        
        // does any of the candidates match?
        for (XLModEntry e : massCandidates) {
            if (e.getReactionsites() == linkedResidues.length)  {
                if (e.getReactionsites() == 1) {
                    boolean isTerm = isProtNterm[0] || isProtCTerm[0] || isPepNterm[0] || isPepCterm[0];
                    for (XLModSpecificity s : e.getSpecificities().get(0)) {
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
                        for (XLModSpecificity s : e.getSpecificities().get(0)) {
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
                            for (XLModSpecificity s : e.getSpecificities().get(0)) {
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
    
    /**
     * tries to find a modification definition based on the information in {@link XLModQuery q}
     * @param q
     * @return a xlmod-entry that fits the definition or null if none can be found
     */
    public XLModEntry guessModification(XLModQuery q) {
        if (q.name != null && !q.name.isEmpty())
            return guessModification(q.mass, q.name, q.linkedResidues, q.isProtNterm, q.isProtCTerm, q.isPepNterm, q.isPepCterm, q.includeNullMass);
        return guessModification(q.mass, q.linkedResidues, q.isProtNterm, q.isProtCTerm, q.isPepNterm, q.isPepCterm);
    }
    
    
    /**
     * tries to find a modification definition based on the information in {@link XLModQuery q}.<br/>
     * this does the same as {@link #guessModification(org.rappsilber.xlmod.XLModQuery) } 
     * but the result will be cached, so that if the same query is requested again 
     * it will return faster with a result.
     * @param q
     * @return a xlmod-entry that fits the definition or null if none can be found
     */ 
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
    
    /**
     * return an entry based on the id.
     * @param id
     * @return 
     */
    public XLModEntry get(String id) {
        if (id.startsWith("XLMOD:")) {
            return idToMod.get(id);
        } else {
            return idToMod.get("XLMOD:"+id);
        }
    }

    /**
     * return an entry based on the id.
     * @param id
     * @return 
     */
    public XLModEntry get(int id) {
        return get(String.format("XLMOD:%05d", id));
    }
    
    public Iterator<XLModEntry> iterator() {
        return list.iterator();
    }

    /**
     * When using the {@link #read(java.lang.String) } method, the source from where the XLMOD was read will be set.
     * This can for example be used to represent the source of XLMOD in an mzIdentML file.
     * @return the usedURL
     */
    public String getUsedURL() {
        return usedURL;
    }

    /**
     * When using the {@link #read(java.lang.String) } method, the source from where the XLMOD was read will be set.
     * This can for example be used to represent the source of XLMOD in an mzIdentML file.
     * @param usedURL the URL used to read in the XLMOD definitions
     */
    public void setUsedURL(String usedURL) {
        this.usedURL = usedURL;
    }
    
    public static void main(String[] args) {
        try {
            XLMOD xlmod = new XLMOD();
            //xlmod.read();
            xlmod = new XLMOD();
                        xlmod.read("https://raw.githubusercontent.com/HUPO-PSI/xlmod-CV/refs/heads/main/XLMOD.obo");
        } catch (IOException ex) {
            Logger.getLogger(XLMOD.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(XLMOD.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
