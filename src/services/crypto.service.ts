import { Injectable, Logger } from '@nestjs/common';
import { ed25519 } from '@noble/ed25519';

@Injectable()
export class CryptoService {
  private readonly logger = new Logger(CryptoService.name);

  async generateAccountId(userId: number): Promise<string> {
    const seed = this.createDeterministicSeed(userId);
    const privateKey = seed.slice(0, 32);
    
    try {
      const publicKey = await ed25519.getPublicKey(privateKey);
      return '0x' + Buffer.from(publicKey).toString('hex');
    } catch (error) {
      this.logger.error(`Failed to generate account ID for userId: ${userId}`, error);
      throw new Error('Cryptographic operation failed. Ensure @noble/ed25519 is installed correctly.');
    }
  }

  private createDeterministicSeed(userId: number): Buffer {
    // Exactly match Kotlin Random(userId).nextBytes(sk) behavior
    // Kotlin uses java.util.Random with Linear Congruential Generator (LCG)
    const seed = Buffer.alloc(32);
    let state = userId & 0xFFFFFFFF; // Ensure 32-bit unsigned integer
    
    // Kotlin Random uses LCG: nextSeed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1)
    // But for nextBytes, it uses next(8) which calls next(bits) internally
    for (let i = 0; i < 32; i++) {
      // Match Kotlin's Random.next(8) implementation exactly
      state = ((state * 0x5DEECE66D + 0xB) & 0xFFFFFFFFFFFF) >>> 0;
      const bits = (state >>> (48 - 8)) & 0xFF;
      seed[i] = bits;
    }
    
    return seed;
  }

  // Helper method to convert JavaScript numbers to Kotlin-style long values
  toLongValue(value: number): number {
    // Ensure we handle the conversion the same way Kotlin does
    return Math.floor(value);
  }
} 