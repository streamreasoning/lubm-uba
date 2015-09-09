/**
 * by Yuanbo Guo
 * Semantic Web and Agent Technology Lab, CSE Department, Lehigh University, USA
 * Copyright (C) 2004
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

package edu.lehigh.swat.bench.uba;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.lehigh.swat.bench.uba.writers.WriterType;

import java.io.*;

public class Generator {

    static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);

    /** delimiter between different parts in an id string */
    public static final char ID_DELIMITER = '/';
    /** delimiter between name and index in a name string of an instance */
    public static final char INDEX_DELIMITER = '_';

    /**
     * Begins the data generation.
     * 
     * @param univNum
     *            Number of universities to generate.
     * @param startIndex
     *            Starting index of the universities.
     * @param seed
     *            Seed for data generation.
     * @param daml
     *            Generates DAML+OIL data if true, OWL data otherwise.
     * @param ontology
     *            Ontology url.
     * @param compress 
     */
    public void start(int univNum, int startIndex, int seed, WriterType writerType, String ontology, String workDir,
            boolean compress, int threads) {
        File outputDir = workDir != null ? new File(workDir) : new File(".");
        outputDir = outputDir.getAbsoluteFile();
        if (!outputDir.exists() || !outputDir.isDirectory()) {
            if (!outputDir.mkdirs()) {
                throw new IllegalArgumentException(
                        String.format("Unable to create requested output directory %s", outputDir));
            }
        }
        GlobalState state = new GlobalState(univNum, seed, startIndex, ontology, writerType, outputDir, compress, threads);

        System.out.println("Started...");

        // Submit a university generator for each university
        List<UniversityState> states = new ArrayList<>();
        for (int i = 0; i < state.getNumberUniversities(); i++) {
            UniversityState univState = new UniversityState(state, i + state.getStartIndex());
            UniversityGenerator univGen = new UniversityGenerator(univState);
            state.getExecutor().submit(univGen);
            states.add(univState);
        }
        try {
            state.getExecutor().shutdown();
            if (!state.getExecutor().awaitTermination(3, TimeUnit.HOURS)) {
                throw new RuntimeException("Timeout was exceeded");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("A generator thread was interrupted", e);
        }

        // Check everything completed
        for (UniversityState univState : states) {
            if (!univState.hasCompleted()) {
                System.err.println("Not all university generators finished successfully, see log for details");
                System.exit(3);
            }
        }

        System.out.println("Completed!");
    }
}