CREATE TABLE [dbo].[UploadSummary]
(
	[UploadSummaryId] INT NOT NULL IDENTITY(1,1) PRIMARY KEY,
	[FileName] VARCHAR(100) NOT NULL,
	[Count] INT NOT NULL,
	[Url] VARCHAR(150) NULL,
	[CreatedAt] DATETIME NOT NULL
);

GO
EXEC sp_addextendedproperty @name = N'MS_Description',
    @value = N'The name of the image file ',
    @level0type = N'SCHEMA',
    @level0name = N'dbo',
    @level1type = N'TABLE',
    @level1name = N'UploadSummary',
    @level2type = N'COLUMN',
    @level2name = N'FileName'
GO
EXEC sp_addextendedproperty @name = N'MS_Description',
    @value = N'The number of times this image appeared irrespective of the name in the window',
    @level0type = N'SCHEMA',
    @level0name = N'dbo',
    @level1type = N'TABLE',
    @level1name = N'UploadSummary',
    @level2type = N'COLUMN',
    @level2name = N'Count'
GO
EXEC sp_addextendedproperty @name = N'MS_Description',
    @value = N'The url of the image uploaded to access from datalake',
    @level0type = N'SCHEMA',
    @level0name = N'dbo',
    @level1type = N'TABLE',
    @level1name = N'UploadSummary',
    @level2type = N'COLUMN',
    @level2name = N'Url'
GO
EXEC sp_addextendedproperty @name = N'MS_Description',
    @value = N'The window end time',
    @level0type = N'SCHEMA',
    @level0name = N'dbo',
    @level1type = N'TABLE',
    @level1name = N'UploadSummary',
    @level2type = N'COLUMN',
    @level2name = N'CreatedAt'