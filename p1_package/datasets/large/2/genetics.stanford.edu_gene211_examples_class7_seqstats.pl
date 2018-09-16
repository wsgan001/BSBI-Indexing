use warnings use strict use bio perl use bio tools seqstats this script will only work if you have an internet connection on the computer you re using the databases you can get sequences from are swiss genbank genpept embl and refseq my sequence_object get_sequence genbank af039652 the script creates the appropriate primary sequence to be passed to seqstats if statistics on a sequence feature are required similarly if a codon count is desired for a frame shifted sequence and or a negative strand sequence the script needs to create that sequence and pass it to the seqstats object http doc bioperl org releases bioperl 1.4 bio tools seqstats html my seq_stats bio tools seqstats new seq sequence_object count_monomers returns a reference to a hash in which keys are letters of the genetic alphabet used and values are number of occurrences of the letter in the sequence my monomer_count seq_stats count_monomers while my monomer occurrences each monomer_count print monomer t occurrences n print n count_codons returns a reference to a hash in which keys are codons of the genetic alphabet used and values are number of occurrences of the codons in the sequence my columns 5 my codon_count seq_stats count_codons my format 0 while my codon occurrences each codon_count print codon t occurrences print format columns 0 n t print n unless format columns 0 print n obtain the molecular weight of a sequence since the sequence may contain ambiguous monomers the molecular weight is returned as a reference to a two element array containing greatest lower bound glb and least upper bound lub of the molecular weight my glb lub 0 1 array result indexes my molecular_weight seq_stats get_mol_wt sequence_object if molecular_weight glb molecular_weight lub print molecular weight is between print molecular_weight glb and molecular_weight lub n else print molecular weight is molecular_weight glb n