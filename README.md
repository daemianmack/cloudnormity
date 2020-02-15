# cloudnormity

A re-do of [conformity](https://github.com/avescodes/conformity) to
work with Datomic Cloud, with some small deviations.

## Deviations from Conformity

- Norms are considered immutable by default and will not be transacted
  a second time unless explicitly marked `:mutable`.

- Conformity's data structure for describing norms allows for norms to
  accumulate transactions over time by appending new items to a given
  norm. This is a confusing aspect of Conformity, in my opinion, and
  allowed editing mistakes to produce inconsistent schema. Here, every
  norm is a single transaction body; one extends past norms by writing
  distinct new ones.

- Conformity's data structure for describing norms was associative.
  Here, it is ordered, freeing us from having to track dependency
  relationships manually.

- Cloudnormity allows reference to resource files for schema; your
  schema doesn't have to live within cloudnormity's confines.

- Cloudnormity allows extension of the mechanism used to resolve
  config references into transaction data; your schema can reside in
  an S3 bucket, encrypted, written in a custom schema DSL.