package org.integratedmodelling.resources.server.controllers;

import org.integratedmodelling.common.services.client.resources.ProjectRequest;
import org.integratedmodelling.klab.api.ServicesAPI;
import org.integratedmodelling.klab.api.collections.Parameters;
import org.integratedmodelling.klab.api.data.Metadata;
import org.integratedmodelling.klab.api.exceptions.KlabInternalErrorException;
import org.integratedmodelling.klab.api.knowledge.Resource;
import org.integratedmodelling.klab.api.knowledge.organization.Project;
import org.integratedmodelling.klab.api.knowledge.organization.ProjectStorage;
import org.integratedmodelling.klab.api.services.ResourcesService;
import org.integratedmodelling.klab.api.services.resources.ResourceSet;
import org.integratedmodelling.klab.services.application.security.EngineAuthorization;
import org.integratedmodelling.klab.services.application.security.Role;
import org.integratedmodelling.klab.services.application.security.ServiceAuthorizationManager;
import org.integratedmodelling.resources.server.ResourcesServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URL;
import java.security.Principal;
import java.util.Collection;
import java.util.List;

@RestController
@Secured(Role.ADMINISTRATOR)
public class ResourceAdminController {

    @Autowired
    private ResourcesServer resourcesServer;

    @Autowired
    private ServiceAuthorizationManager authenticationManager;

    @PostMapping(ServicesAPI.RESOURCES.ADMIN.IMPORT_PROJECT)
    public @ResponseBody ResourceSet importNewProject(@RequestBody ProjectRequest request,
                                                      Principal principal) {
        if (resourcesServer.klabService() instanceof ResourcesService.Admin admin) {
            return admin.importProject(request.getWorkspaceName(), request.getProjectUrl(),
                    request.isOverwrite(), authenticationManager.resolveScope(principal));
        }
        throw new KlabInternalErrorException("Resources service is incapable of admin operation");
    }

    @PostMapping(ServicesAPI.RESOURCES.ADMIN.CREATE_PROJECT)
    public @ResponseBody ResourceSet createNewProject(@PathVariable("workspaceName") String workspaceName,
                                                      @PathVariable(
            "projectName") String projectName, Principal principal) {
        if (resourcesServer.klabService() instanceof ResourcesService.Admin admin) {
            return admin.createProject(workspaceName, projectName,
                    authenticationManager.resolveScope(principal));
        }
        throw new KlabInternalErrorException("Resources service is incapable of admin operation");
    }

    @PostMapping(ServicesAPI.RESOURCES.ADMIN.UPDATE_PROJECT)
    public @ResponseBody ResourceSet updateExistingProject(@PathVariable("projectName") String projectName,
                                                           @RequestBody Project.Manifest manifest,
                                                           @RequestBody Metadata metadata,
                                                           Principal principal) {
        if (resourcesServer.klabService() instanceof ResourcesService.Admin admin && principal instanceof EngineAuthorization auth) {
            return admin.updateProject(projectName, manifest, metadata, auth.getToken());
        }
        throw new KlabInternalErrorException("Resources service is incapable of admin operation");
    }

    @PostMapping(ServicesAPI.RESOURCES.ADMIN.CREATE_DOCUMENT)
    public List<ResourceSet> createDocument(@PathVariable("projectName") String projectName,
                                            @PathVariable("documentType") ProjectStorage.ResourceType documentType,
                                            @PathVariable("urn") String urn, Principal principal) {
        if (resourcesServer.klabService() instanceof ResourcesService.Admin admin && principal instanceof EngineAuthorization auth) {
            return admin.createDocument(projectName, urn, documentType, auth.getToken());
        }
        throw new KlabInternalErrorException("Resources service is incapable of admin operation");
    }

    @PostMapping(ServicesAPI.RESOURCES.ADMIN.UPDATE_DOCUMENT)
    public List<ResourceSet> updateOntology(@PathVariable("projectName") String projectName,
                                            @PathVariable("documentType") ProjectStorage.ResourceType documentType,
                                            @RequestBody String content, Principal principal) {
        if (resourcesServer.klabService() instanceof ResourcesService.Admin admin && principal instanceof EngineAuthorization auth) {
            return admin.updateDocument(projectName, documentType, content, auth.getToken());
        }
        throw new KlabInternalErrorException("Resources service is incapable of admin operation");
    }

    @PostMapping(ServicesAPI.RESOURCES.ADMIN.IMPORT_RESOURCE)
    public @ResponseBody ResourceSet createResource(@RequestBody Resource resource, Principal principal) {
        if (resourcesServer.klabService() instanceof ResourcesService.Admin admin) {
            var urn = admin.createResource(resource, authenticationManager.resolveScope(principal));
            return null; // TODO create ResourceSet
        }
        throw new KlabInternalErrorException("Resources service is incapable of admin operation");
    }

    @PostMapping(ServicesAPI.RESOURCES.ADMIN.UPLOAD_RESOURCE)
    public @ResponseBody ResourceSet createResourceFromPath(@RequestBody File resourcePath,
                                                            Principal principal) {
        if (resourcesServer.klabService() instanceof ResourcesService.Admin admin) {
            var urn = admin.createResource(resourcePath, authenticationManager.resolveScope(principal));
            return null; // TODO create ResourceSet
        }
        throw new KlabInternalErrorException("Resources service is incapable of admin operation");
    }

    @PostMapping(ServicesAPI.RESOURCES.ADMIN.CREATE_RESOURCE)
    public Resource createResourceForProject(@RequestParam("projectName") String projectName,
                                             @RequestParam("urnId") String urnId,
                                             @RequestParam("adapter") String adapter,
                                             @RequestBody Parameters<String> resourceData,
                                             Principal principal) {
        if (resourcesServer.klabService() instanceof ResourcesService.Admin admin) {
            return admin.createResource(projectName, urnId, adapter, resourceData,
                    authenticationManager.resolveScope(principal));
        }
        throw new KlabInternalErrorException("Resources service is incapable of admin operation");
    }

    @PostMapping(ServicesAPI.RESOURCES.ADMIN.REMOVE_PROJECT)
    public List<ResourceSet> removeProject(@RequestParam("projectName") String projectName) {
        if (resourcesServer.klabService() instanceof ResourcesService.Admin admin) {
            admin.removeProject(projectName);
        }
        throw new KlabInternalErrorException("Resources service is incapable of admin operation");
    }

    @PostMapping(ServicesAPI.RESOURCES.ADMIN.REMOVE_WORKSPACE)
    public List<ResourceSet> removeWorkspace(@RequestParam("workspaceName") String workspaceName) {
        if (resourcesServer.klabService() instanceof ResourcesService.Admin admin) {
            admin.removeWorkspace(workspaceName);
        }
        throw new KlabInternalErrorException("Resources service is incapable of admin operation");
    }

    @GetMapping(value = ServicesAPI.RESOURCES.LIST_PROJECTS, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody Collection<Project> listProjects(Principal principal) {
        if (resourcesServer.klabService() instanceof ResourcesService.Admin admin) {
            return admin.listProjects(authenticationManager.resolveScope(principal));
        }
        throw new KlabInternalErrorException("Resources service is incapable of admin operation");
    }

    @GetMapping(value = ServicesAPI.RESOURCES.LIST_RESOURCE_URNS, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody Collection<String> listResourceUrns(Principal principal) {
        if (resourcesServer.klabService() instanceof ResourcesService.Admin admin) {
            return admin.listResourceUrns(authenticationManager.resolveScope(principal));
        }
        throw new KlabInternalErrorException("Resources service is incapable of admin operation");
    }

    @GetMapping(value = ServicesAPI.RESOURCES.ADMIN.LOCK_PROJECT)
    public URL lockProject(@PathVariable("urn") String urn, Principal principal) {
        if (resourcesServer.klabService() instanceof ResourcesService.Admin admin && principal instanceof EngineAuthorization auth) {
            return admin.lockProject(urn, auth.getToken());
        }
        throw new KlabInternalErrorException("Resources service is incapable of admin operation");
    }

    @GetMapping(value = ServicesAPI.RESOURCES.ADMIN.UNLOCK_PROJECT)
    public boolean unlockProject(@PathVariable("urn") String urn, Principal principal) {
        if (resourcesServer.klabService() instanceof ResourcesService.Admin admin && principal instanceof EngineAuthorization auth) {
            return admin.unlockProject(urn, auth.getToken());
        }
        throw new KlabInternalErrorException("Resources service is incapable of admin operation");
    }

    @PostMapping(value = ServicesAPI.RESOURCES.ADMIN.MANAGE_PROJECT)
    public ResourceSet manageProject(@PathVariable("urn") String urn, @RequestBody ProjectRequest request,
                                     Principal principal) {
        if (resourcesServer.klabService() instanceof ResourcesService.Admin admin) {
            return admin.manageRepository(urn, request.getOperation(),
                    request.getParameters().toArray(new String[]{}));
        }
        throw new KlabInternalErrorException("Resources service is incapable of admin operation");
    }


}
