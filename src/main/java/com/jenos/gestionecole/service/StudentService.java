package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.*;
import com.jenos.gestionecole.repository.*;

import java.util.List;

/**
 * StudentService — Logique métier et coordination de la gestion des étudiants.
 *
 * Architecture :
 * StudentsController
 *        ↓
 * StudentService
 *        ↓
 * StudentRepository
 *        ↓
 * Database
 */
public class StudentService {

    private static StudentService instance;
    private final StudentRepository studentRepository;

    public enum SortCriteria {
        TRI_DEFAUT("Tri par défaut"),
        NOM_ASC("Nom A → Z"),
        NOM_DESC("Nom Z → A"),
        MATRICULE("Matricule"),
        PROMOTION("Promotion"),
        OPTION_ASC("Option A → Z"),
        OPTION_DESC("Option Z → A"),
        CLASSE_ASC("Classe A → Z"),
        DATE_INSCRIPTION("Date d'inscription");

        private final String label;

        SortCriteria(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static class SearchCriteria {
        public String keyword;          // Nom, postnom, prénom, matricule, téléphone
        public String anneeScolaire;
        public Section section;
        public Promotion promotion;
        public AcademicOption option;
        public Classe classe;
        public Student.StudentStatus statut;
        public SortCriteria sortBy = SortCriteria.TRI_DEFAUT;
        public int page = 1;
        public int pageSize = 20;
    }

    private StudentService() {
        SectionRepository secRepo = new SectionRepository();
        PromotionRepository promoRepo = new PromotionRepository(secRepo);
        OptionRepository optRepo = new OptionRepository(promoRepo);
        ClasseRepository classeRepo = new ClasseRepository(optRepo, promoRepo);
        this.studentRepository = new StudentRepository(classeRepo);
    }

    public static StudentService getInstance() {
        if (instance == null) {
            instance = new StudentService();
        }
        return instance;
    }

    public StudentSearchResult search(StudentFilter filter) {
        return studentRepository.search(filter);
    }

    public List<Student> search(SearchCriteria criteria) {
        StudentFilter filter = toFilter(criteria);
        return studentRepository.search(filter).getItems();
    }

    public int count(SearchCriteria criteria) {
        StudentFilter filter = toFilter(criteria);
        return studentRepository.search(filter).getTotalCount();
    }

    private StudentFilter toFilter(SearchCriteria criteria) {
        StudentFilter filter = new StudentFilter();
        if (criteria != null) {
            filter.setSearchTerm(criteria.keyword);
            filter.setAcademicYear(criteria.anneeScolaire);
            filter.setSection(criteria.section);
            filter.setPromotion(criteria.promotion);
            filter.setOption(criteria.option);
            filter.setClasse(criteria.classe);
            filter.setStatus(criteria.statut);
            filter.setPage(criteria.page > 0 ? criteria.page : 1);
            filter.setPageSize(criteria.pageSize > 0 ? criteria.pageSize : 500);

            if (criteria.sortBy != null) {
                switch (criteria.sortBy) {
                    case NOM_ASC -> filter.setSort(StudentFilter.SortField.NOM, true);
                    case NOM_DESC -> filter.setSort(StudentFilter.SortField.NOM, false);
                    case MATRICULE -> filter.setSort(StudentFilter.SortField.MATRICULE, true);
                    case PROMOTION -> filter.setSort(StudentFilter.SortField.PROMOTION, true);
                    case OPTION_ASC -> filter.setSort(StudentFilter.SortField.OPTION, true);
                    case OPTION_DESC -> filter.setSort(StudentFilter.SortField.OPTION, false);
                    case CLASSE_ASC -> filter.setSort(StudentFilter.SortField.CLASSE, true);
                    case DATE_INSCRIPTION, TRI_DEFAUT -> filter.setSort(StudentFilter.SortField.NOM, true);
                }
            }
        }
        return filter;
    }

    public List<Student> searchByKeyword(String keyword) {
        StudentFilter filter = new StudentFilter();
        filter.setSearchTerm(keyword);
        filter.setPageSize(500);
        return studentRepository.search(filter).getItems();
    }

    public void save(Student student) {
        studentRepository.save(student);
        DashboardService.getInstance().notifyDataChanged();
    }

    public void deactivate(int studentId) {
        studentRepository.deactivate(studentId);
        DashboardService.getInstance().notifyDataChanged();
    }

    public void archive(int studentId) {
        studentRepository.archive(studentId);
    }

    public Student findById(int id) {
        return studentRepository.findById(id);
    }

    public boolean isMatriculeUnique(String matricule, int excludeId) {
        return !studentRepository.existsByMatricule(matricule, excludeId);
    }

    public boolean isAlreadyEnrolledInYear(String nom, String postnom, String anneeScolaire, int excludeId) {
        return studentRepository.isAlreadyEnrolledInYear(nom, postnom, anneeScolaire, excludeId);
    }

    public String generateMatricule(String anneeScolaire, Section section) {
        String year = (anneeScolaire != null && anneeScolaire.length() >= 4)
                ? anneeScolaire.substring(0, 4) : "2025";
        String sec = section != null
                ? section.getName().substring(0, Math.min(3, section.getName().length())).toUpperCase()
                : "SEC";
        int seq = studentRepository.countTotal() + 1;
        return String.format("%s-%s-%04d", year, sec, seq);
    }
}
