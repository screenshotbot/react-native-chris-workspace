/**
 * Converts a story name like "MyFeature/Initial" to Storybook's ID format "myfeature--initial"
 */
export function storyNameToId(storyName: string): string {
  const [title, name] = storyName.split('/');
  const titleKebab = title.toLowerCase().replace(/\s+/g, '-');
  const nameKebab = name.toLowerCase().replace(/\s+/g, '-');
  return `${titleKebab}--${nameKebab}`;
}
