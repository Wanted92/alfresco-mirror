--
-- Title:      Post-Create Indexes
-- Database:   MySQL
-- Since:      V2.2 Schema 86
-- Author:     Derek Hulley
--
-- Hibernate only generates indexes on foreign key columns for MySQL.
--
-- Please contact support@alfresco.com if you need assistance with the upgrade.
--

--
-- Record script finish
--
DELETE FROM alf_applied_patch WHERE id = 'patch.db-V2.2-5-MappedFKIndexes';
INSERT INTO alf_applied_patch
  (id, description, fixes_from_schema, fixes_to_schema, applied_to_schema, target_schema, applied_on_date, applied_to_server, was_executed, succeeded, report)
  VALUES
  (
    'patch.db-V2.2-5-MappedFKIndexes', 'Manually executed script upgrade V2.2: Created FK indexes',
    0, 120, -1, 121, null, 'UNKOWN', 1, 1, 'Script completed'
  );
