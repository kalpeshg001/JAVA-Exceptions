package com.ipiecoles.java.java230;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.model.Commercial;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;

@Component
public class MyRunner implements CommandLineRunner {

	private static final String REGEX_MATRICULE = "^[MTC][0-9]{5}$";
	private static final String REGEX_NOM = ".*";
	private static final String REGEX_PRENOM = ".*";
	private static final int NB_CHAMPS_MANAGER = 5;
	private static final int NB_CHAMPS_TECHNICIEN = 7;
	private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
	private static final int NB_CHAMPS_COMMERCIAL = 7;
	private static final String sal = "";

	@Autowired
	private EmployeRepository employeRepository;

	@Autowired
	private ManagerRepository managerRepository;

	private List<Employe> employes = new ArrayList<Employe>();

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	public void run(String... strings) {
		try {
			String fileName = "employes.csv";
			readFile(fileName);
		} catch (Exception e) {
			
			logger.error("Le fichier n'est pas disponible dans l'URL fourni =>" + e.getMessage());
		}		
	}

	/**
	 * Méthode qui lit le fichier CSV en paramètre afin d'intégrer son contenu en
	 * BDD
	 * 
	 * @param fileName Le nom du fichier (à mettre dans src/main/resources)
	 * @return une liste contenant les employés à insérer en BDD ou null si le
	 *         fichier n'a pas pu être le
	 */
	public List<Employe> readFile(String fileName) throws Exception {
		Stream<String> stream;
		stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
		// TODO
		Integer i = 0;

		for (String ligne : stream.collect(Collectors.toList())) {
			i++;
			try {
				processLine(ligne);
			} catch (BatchException e) {
				System.out.println("ligne" + i + ":" + e.getMessage() + "=> " + ligne);
			}
		}
		return employes;
	}

	/**
	 * Méthode qui regarde le premier caractère de la ligne et appelle la bonne
	 * méthode de création d'employé
	 * 
	 * @param ligne la ligne à analyser
	 * @throws BatchException si le type d'employé n'a pas été reconnu
	 */
	private void processLine(String ligne) throws BatchException {
		// Split each line in table of strings separated by ",". This is ideal to compare each conditions by calling line[0/1/2..etc]
		String[] line = ligne.split(",");

		//Verifies the first letter in the Matricule if it matches
		if (!ligne.matches("^[MCT]{1}.*")) {
			throw new BatchException("Type d'emploi inconnu: " + ligne.charAt(0) + " ");
		}
		//Verifies the first object in the table line[] if it is REGEX_MATRICULE
		if (!line[0].matches(REGEX_MATRICULE)) {
			throw new BatchException("la chaîne " + line[0] +" ne respecte pas l'expression régulière ^[MTC][0-9]{5}$ ");
		}
		//all three below, Verifies each matricule, verifiesseparate M/T/C and compare the length of the table to that of each M/T/C 
		if (line[0].matches(REGEX_MATRICULE) && ligne.matches("^[M]{1}.*") && line.length != NB_CHAMPS_MANAGER) {
			throw new BatchException("La ligne manager ne contient pas 5 éléments mais " + line.length + " ");
		}
		if (line[0].matches(REGEX_MATRICULE) && ligne.matches("^[T]{1}.*") && line.length != NB_CHAMPS_TECHNICIEN) {
			throw new BatchException("La ligne technicien ne contient pas 7 éléments mais " + line.length + " ");
		}
		if (line[0].matches(REGEX_MATRICULE) && ligne.matches("^[C]{1}.*") && line.length != NB_CHAMPS_COMMERCIAL) {
			throw new BatchException("La ligne commercial ne contient pas 7 éléments mais " + line.length + " ");
		}
		//Parse the string to LocalDate with pattern and compare with a right format
		try {
			// LocalDate d = new LocalDate(dateFormat.parse(date));
			LocalDate d = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(line[3]);
		} catch (Exception e) {
			throw new BatchException(line[3] + " ne respecte pas le format de date dd/MM/yyyy ");
		}
		//Parse the object to verify if it is a DOUBLE number or not, if not give error message
		try {
			Double.parseDouble(line[4]);
		} catch (Exception e) {
			throw new BatchException(line[4] + " n'est pas un nombre valide pour un salaire ");
		}
		//Verify if matricule starts with "C", followed by Parse the object to verify if it is a DOUBLE number or not, if not give error message
		if (ligne.matches("^[C]{1}.*")) {
			try {
				Double.parseDouble(line[5]);
			} catch (Exception e) {
				throw new BatchException("Le chiffre d'affaire du commercial est incorrect : " + line[5]);
			}
		}
		//Verify if matricule starts with "C", followed by Parse the object to verify if it is a DOUBLE number or not, if not give error message
		if (ligne.matches("^[C]{1}.*")) {
			try {
				Double.parseDouble(line[6]);
			} catch (Exception e) {
				throw new BatchException("La performance du commercial est incorrecte : " + line[6]);
			}
		}
		//Verify if matricule starts with "T", followed by Parse the object to verify if it is an Integer, if not give error message
		if (ligne.matches("^[T]{1}.*")) {
			try {
				Integer grade = Integer.parseInt(line[5]);							
			} catch (Exception e) {
				throw new BatchException("Le grade du technicien est incorrect :  " + line[5] + " ");
			}
		}
		//Verify if matricule starts with "T" followed by comparison to validate if the number is between 1 & 5, if not give error message		
		if (ligne.matches("^[T]{1}.*") && !line[5].matches("^[1-5]{1}")) {			
				throw new BatchException("Le grade doit être compris entre 1 et 5 : " + line[5] + " ");			
		}	
		//Verify if matricule starts with "T" and compare Manager's matricule respect the REGEX we have declared
		if (ligne.matches("^[T]{1}.*") && !line[6].matches(REGEX_MATRICULE)) {
			throw new BatchException ("la chaîne " + line[6] + " ne respecte pas l'expression régulière ^M[0-9]{5}$ ");
		}
		//Verify if matricule starts with "T" and by calling to employeRepository confirm whether "Manager" with given matricule exists in BDD.
		if (ligne.matches("^[T]{1}.*") && employeRepository.findByMatricule(line[6])==null) {
			throw new BatchException ("Le manager de matricule " + line[6] + " n'a pas été trouvé dans le fichier ou en base de données ");
		}				

	}

	/**
	 * Méthode qui crée un Commercial à partir d'une ligne contenant les
	 * informations d'un commercial et l'ajoute dans la liste globale des employés
	 * 
	 * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
	 * @throws BatchException s'il y a un problème sur cette ligne
	 */
	private void processCommercial(String ligneCommercial) throws BatchException {
		// TODO
	}

	/**
	 * Méthode qui crée un Manager à partir d'une ligne contenant les informations
	 * d'un manager et l'ajoute dans la liste globale des employés
	 * 
	 * @param ligneManager la ligne contenant les infos du manager à intégrer
	 * @throws BatchException s'il y a un problème sur cette ligne
	 */
	private void processManager(String ligneManager) throws BatchException {
		// TODO
	}

	/**
	 * Méthode qui crée un Technicien à partir d'une ligne contenant les
	 * informations d'un technicien et l'ajoute dans la liste globale des employés
	 * 
	 * @param ligneTechnicien la ligne contenant les infos du technicien à intégrer
	 * @throws BatchException s'il y a un problème sur cette ligne
	 */
	private void processTechnicien(String ligneTechnicien) throws BatchException {
		// TODO
	}

}
