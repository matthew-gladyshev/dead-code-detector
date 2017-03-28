package com.dgladyshev.deadcodedetector.controllers;

import static org.apache.commons.lang.StringUtils.trimToEmpty;

import com.dgladyshev.deadcodedetector.entity.GitRepo;
import com.dgladyshev.deadcodedetector.entity.Inspection;
import com.dgladyshev.deadcodedetector.entity.SupportedLanguages;
import com.dgladyshev.deadcodedetector.exceptions.MalformedRequestException;
import com.dgladyshev.deadcodedetector.repositories.InspectionsRepository;
import com.dgladyshev.deadcodedetector.services.InspectionService;
import com.dgladyshev.deadcodedetector.services.UrlCheckerService;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/")
public class DeadCodeController {

    private final InspectionService inspectionService;
    private final InspectionsRepository inspectionsRepository;
    private final UrlCheckerService urlCheckerService;

    @Autowired
    public DeadCodeController(InspectionService inspectionService,
                              InspectionsRepository inspectionsRepository,
                              UrlCheckerService urlCheckerService) {
        this.inspectionService = inspectionService;
        this.inspectionsRepository = inspectionsRepository;
        this.urlCheckerService = urlCheckerService;
    }

    @RequestMapping(
            value = "/inspections",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Inspection addInspection(@RequestParam String url,
                                    @RequestParam SupportedLanguages language,
                                    @RequestParam(defaultValue = "master") String branch) {
        log.info("Incoming request for analysis, url: {}, language: {}, branch: {}", url, language, branch);
        GitRepo gitRepo = new GitRepo(url);
        urlCheckerService.checkAccessibility(
                gitRepo.getUrl().replace(".git", "")
        );
        Inspection inspection = inspectionsRepository.createInspection(
                gitRepo,
                trimToEmpty(branch),
                language.getName()
        );
        inspectionService.inspectCode(inspection.getInspectionId());
        return inspection;
    }

    @RequestMapping(
            value = "/inspections/refresh",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void refreshInspection(@RequestParam String url,
                                  @RequestParam(defaultValue = "master") String branch) {
        log.info("Incoming request for refreshing an inspection, url: {}, branch: {}", url, branch);
        GitRepo gitRepo = new GitRepo(url);
        Inspection inspection = inspectionsRepository.getRefreshableInspection(gitRepo, trimToEmpty(branch));
        inspectionService.inspectCode(inspection.getInspectionId());
    }

    @RequestMapping(
            value = "/inspections",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Collection<Inspection> getInspections(@RequestParam(required = false) Long pageNumber,
                                                 @RequestParam(required = false) Long pageSize) {
        if (pageNumber != null && pageSize != null) {
            if (pageNumber < 1 || pageSize < 0) {
                throw new MalformedRequestException("Page number must equal or bigger than 1,"
                                                    + " page size must be bigger than 0");
            } else {
                return inspectionsRepository.getInspections()
                        .values()
                        .stream()
                        .skip((pageNumber - 1) * pageSize)
                        .limit(pageSize)
                        .collect(Collectors.toList());
            }
        } else {
            return inspectionsRepository.getInspections().values();
        }
    }

    @RequestMapping(
            value = "/inspections/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Inspection getInspectionById(@PathVariable String id,
                                        @RequestParam(defaultValue = "", required = false) String filter) {
        Inspection inspection = inspectionsRepository.getInspection(id);
        return (StringUtils.isEmptyOrNull(filter))
               ? inspection
               : inspection.toFilteredInspection(filter);
    }

    @RequestMapping(
            value = "/inspections/{id}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void deleteInspectionById(@PathVariable String id) {
        inspectionsRepository.deleteInspection(id);
    }

}

