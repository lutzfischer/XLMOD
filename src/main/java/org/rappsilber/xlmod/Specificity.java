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

/**
 *
 * @author lfischer
 */
public class Specificity {
    public static String PROTEIN_C_TERMINAL_STRING = "Protein C-Term";
    public static String PROTEIN_N_TERMINAL_STRING = "Protein N-Term";
    public static String PEPTIDE_C_TERMINAL_STRING = "C-Term";
    public static String PEPTIDE_N_TERMINAL_STRING = "N-Term";
    public static String NON_SPECIFIC = "*";
    
    public String AminoAcid = NON_SPECIFIC;
    public boolean ProteinNterminal = false;
    public boolean ProteinCterminal = false;
    public boolean PeptideNterminal = false;
    public boolean PeptideCterminal = false;
    public boolean terminalRestricted = false;
    public boolean isNonSpecific = false;

    public Specificity() {
    }

    public Specificity(String AminoAcid, boolean ProteinNterminal, boolean ProteinCterminal, boolean PeptideNterminal, boolean PeptideCterminal) {
        this.AminoAcid = AminoAcid;
        this.ProteinNterminal = ProteinNterminal;
        this.ProteinCterminal = ProteinCterminal;
        this.PeptideNterminal = PeptideNterminal;
        this.PeptideCterminal = PeptideCterminal;
        terminalRestricted = ProteinNterminal || ProteinCterminal || PeptideNterminal  || PeptideCterminal;
        isNonSpecific = AminoAcid.contentEquals(NON_SPECIFIC);
    }

    public Specificity(String config) {
        config=config.trim();
        
        //do we have some terminal specificity
        if (config.toLowerCase().contains("term")) {
            if (config.toLowerCase().contains(PROTEIN_C_TERMINAL_STRING)) {
                this.ProteinCterminal = true;
                config.replace(PROTEIN_C_TERMINAL_STRING, "");
            } else if (config.toLowerCase().contains(PROTEIN_N_TERMINAL_STRING)) {
                this.ProteinNterminal = true;
                config.replace(PROTEIN_N_TERMINAL_STRING, "");
            } else if (config.toLowerCase().contains(PEPTIDE_C_TERMINAL_STRING)) {
                this.PeptideCterminal = true;
                config.replace(PEPTIDE_C_TERMINAL_STRING, "");
            } else if (config.toLowerCase().contains(PEPTIDE_N_TERMINAL_STRING)) {
                this.PeptideNterminal = true;
                config.replace(PEPTIDE_N_TERMINAL_STRING, "");
            } 
            config=config.trim();
        }
        
        if (config.isEmpty()) {
            AminoAcid = NON_SPECIFIC;
        } else {
            AminoAcid = config;
        }
    }    
}
