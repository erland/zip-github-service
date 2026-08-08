package info.isaksson.erland.zipgithub.domain;

import info.isaksson.erland.zipgithub.domain.model.StagingImport;
import info.isaksson.erland.zipgithub.domain.status.StagingImportStatus;
import info.isaksson.erland.zipgithub.domain.status.DomainTransitionException;
import info.isaksson.erland.zipgithub.upload.*;
import java.nio.file.Path; import java.time.Instant; import java.util.Map; import java.util.UUID;
public final class StagingImportLifecycleSelfTest { public static void main(String[] args){
  Instant n=Instant.parse("2026-08-08T04:00:00Z"); UUID o=UUID.randomUUID(),i=UUID.randomUUID();
  var a=new StoredUploadArtifact(UUID.randomUUID(),"p.zip",2,"a".repeat(64),Path.of("/tmp/p"),n,n.plusSeconds(60),Map.of("run",GitFileMode.EXECUTABLE));
  var s=new StagingImport(UUID.randomUUID(),a,"b".repeat(64),n,n.plusSeconds(60));
  if(!s.claim(o,n.plusSeconds(1),n.plusSeconds(300))||s.claim(o,n.plusSeconds(2),n.plusSeconds(600))||!s.expiresAt().equals(n.plusSeconds(300))||!s.promote(o,i,n.plusSeconds(3))||s.promote(o,i,n.plusSeconds(4))||s.status()!=StagingImportStatus.PROMOTED) throw new AssertionError();
  var expired=new StagingImport(UUID.randomUUID(),a,"c".repeat(64),n,n.plusSeconds(60)); expired.expire(n.plusSeconds(61));
  try { expired.claim(UUID.randomUUID(),n.plusSeconds(62)); throw new AssertionError("terminal claim must fail"); } catch (DomainTransitionException expected) {}
  if(GitFileMode.fromUnixMode(0)!=null||GitFileMode.fromUnixMode(0100755)!=GitFileMode.EXECUTABLE) throw new AssertionError();
  System.out.println("StagingImport lifecycle self-test passed."); }}
