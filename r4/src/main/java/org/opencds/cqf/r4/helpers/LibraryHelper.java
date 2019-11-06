package org.opencds.cqf.r4.helpers;

import java.util.ArrayList;
import java.util.List;

import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.common.evaluation.LibraryLoader;
import org.opencds.cqf.common.evaluation.MeasureTranslationOptionsLibraryManager;
import org.opencds.cqf.common.providers.LibraryResolutionProvider;
import org.opencds.cqf.common.providers.LibrarySourceProvider;

/**
 * Created by Christopher on 1/11/2017.
 */
public class LibraryHelper {

    public static LibraryLoader createLibraryLoader(LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> provider) {
        ModelManager modelManager = new ModelManager();
        LibraryManager libraryManager = new MeasureTranslationOptionsLibraryManager(modelManager);
        libraryManager.getLibrarySourceLoader().clearProviders();
        
        libraryManager.getLibrarySourceLoader().registerProvider(
            new LibrarySourceProvider<org.hl7.fhir.r4.model.Library, org.hl7.fhir.r4.model.Attachment>(
                provider, 
                x -> x.getContent(),
                x -> x.getContentType(),
                x -> x.getData()));

        return new LibraryLoader(libraryManager, modelManager);
    }


    public static List<org.cqframework.cql.elm.execution.Library> loadLibraries(Measure measure, org.opencds.cqf.cql.execution.LibraryLoader libraryLoader, LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> libraryResourceProvider)
    {
        List<org.cqframework.cql.elm.execution.Library> libraries = new ArrayList<org.cqframework.cql.elm.execution.Library>();

        // load libraries
          // load libraries
          for (CanonicalType ref : measure.getLibrary()) {
            // if library is contained in measure, load it into server
            if (ref.getId().startsWith("#")) {
                for (Resource resource : measure.getContained()) {
                    if (resource instanceof org.hl7.fhir.r4.model.Library
                            && resource.getIdElement().getIdPart().equals(ref.getId().substring(1)))
                    {
                        libraryResourceProvider.update((org.hl7.fhir.r4.model.Library) resource);
                    }
                }
            }

            // We just loaded it into the server so we can access it by Id
            String id = ref.getId();
            if (id.startsWith("#")) {
                id = id.substring(1);
            }

            org.hl7.fhir.r4.model.Library library = libraryResourceProvider.resolveLibraryById(id);
            libraries.add(
                libraryLoader.load(new VersionedIdentifier().withId(library.getName()).withVersion(library.getVersion()))
            );
        }

        for (RelatedArtifact artifact : measure.getRelatedArtifact()) {
            if (artifact.hasType() && artifact.getType().equals(RelatedArtifact.RelatedArtifactType.DEPENDSON) && artifact.hasResource() && artifact.hasResource()) {
                org.hl7.fhir.r4.model.Library library = libraryResourceProvider.resolveLibraryById(artifact.getResource());
                libraries.add(
                    libraryLoader.load(new VersionedIdentifier().withId(library.getName()).withVersion(library.getVersion()))
                );
            }
        }

        if (libraries.isEmpty()) {
            throw new IllegalArgumentException(String
                    .format("Could not load library source for libraries referenced in Measure/%s.", measure.getId()));
        }

        return libraries;
    }

    public static Library resolveLibraryById(String libraryId, org.opencds.cqf.cql.execution.LibraryLoader libraryLoader, LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> libraryResourceProvider)
    {
        // Library library = null;

        org.hl7.fhir.r4.model.Library fhirLibrary = libraryResourceProvider.resolveLibraryById(libraryId);
        return libraryLoader.load(new VersionedIdentifier().withId(fhirLibrary.getName()).withVersion(fhirLibrary.getVersion()));

        // for (Library l : libraryLoader.getLibraries()) {
        //     VersionedIdentifier vid = l.getIdentifier();
        //     if (vid.getId().equals(fhirLibrary.getName()) && LibraryResourceHelper.compareVersions(fhirLibrary.getVersion(), vid.getVersion()) == 0) {
        //         library = l;
        //         break;
        //     }
        // }

        // if (library == null) {

        // }

        // return library;
    }

    public static Library resolvePrimaryLibrary(Measure measure, org.opencds.cqf.cql.execution.LibraryLoader libraryLoader, LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> libraryResourceProvider)
    {
        // default is the first library reference
        String id = measure.getLibrary().get(0).getId();

        Library library = resolveLibraryById(id, libraryLoader, libraryResourceProvider);

        if (library == null) {
            throw new IllegalArgumentException(String
                    .format("Could not resolve primary library for Measure/%s.", measure.getIdElement().getIdPart()));
        }

        return library;
    }

    public static Library resolvePrimaryLibrary(PlanDefinition planDefinition, LibraryLoader libraryLoader, LibraryResolutionProvider<org.hl7.fhir.r4.model.Library> libraryResourceProvider) {
        String id = CanonicalHelper.getId(planDefinition.getLibrary().get(0));

        Library library = resolveLibraryById(id, libraryLoader, libraryResourceProvider);

        if (library == null) {
            throw new IllegalArgumentException(String.format("Could not resolve primary library for PlanDefinition/%s", planDefinition.getIdElement().getIdPart()));
        }

        return library;
    }
}
